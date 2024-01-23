package io.odin.ta4s.backtest.strategy

import enumeratum.{Enum, EnumEntry}
import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.backtest.strategy.ADXShortOptimizedStrategy.ADXCrossesState.{
  PlusDICrossedDownMinusDI,
  PlusDICrossedUpMinusDI,
  Unknown
}
import io.odin.ta4s.backtest.strategy.ADXShortOptimizedStrategy.{ADXCrossesState, StrategyInternalState}
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.Rule
import org.ta4j.core.indicators._
import org.ta4j.core.indicators.adx.{MinusDIIndicator, PlusDIIndicator}
import org.ta4j.core.indicators.helpers.{DifferenceIndicator, PreviousValueIndicator}
import org.ta4j.core.trading.rules.{
  CrossedDownIndicatorRule,
  CrossedUpIndicatorRule,
  OverIndicatorRule,
  UnderIndicatorRule
}

class ADXShortOptimizedStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends BackTestStrategy {

  val series = closePriceIndicator.indicator.getBarSeries
  val closePrice = closePriceIndicator.indicator

  var strategyInternalState: StrategyInternalState = StrategyInternalState()

  val smaIndicator = new SMAIndicator(closePriceIndicator.indicator, 50)

  val adxBarCount = 14
//  val adxIndicator = new ADXIndicator(series, adxBarCount)
  val adxIndicator = new OdinADXIndicator(series, adxBarCount)

  val adxOver20Rule = new OverIndicatorRule(adxIndicator, 20)
  val adxOver10Rule = new OverIndicatorRule(adxIndicator, 10)

  val slopeIndicator = new SlopeIndicator(adxIndicator)
  val adxSlopeUnderMinusOne = new UnderIndicatorRule(slopeIndicator, -1)

  val adxSlopePrev = new PreviousValueIndicator(slopeIndicator, 1)
  val adxSlopeDiff = new DifferenceIndicator(slopeIndicator, adxSlopePrev)

  val adxSlopeDiffUnderMinusOne = new UnderIndicatorRule(adxSlopeDiff, -1)
  val adxSlopeDiffUnderOne = new UnderIndicatorRule(adxSlopeDiff, 1)
  val adxSlopeDiffOverOne = new OverIndicatorRule(adxSlopeDiff, 1)

  val stochasticOscill = new StochasticOscillatorKIndicator(series, 14)
  val stochasticOscillK = new SMAIndicator(stochasticOscill, 3)

  val overSoldRule = new UnderIndicatorRule(stochasticOscillK, 20)
  val overBoughtRule = new OverIndicatorRule(stochasticOscillK, 80)
  val stochUnder80Rule = new UnderIndicatorRule(stochasticOscillK, 80)
  val stochOver40Rule = new OverIndicatorRule(stochasticOscillK, 40)

  val macdIndicator = new OdinMACDIndicator(closePrice)
  val macdLineCrossUpSignalRule =
    new CrossedUpIndicatorRule(macdIndicator.getMACDLineIndicator, macdIndicator.getSignalIndicator)

  val macdLineCrossDownSignalRule =
    new CrossedDownIndicatorRule(macdIndicator.getMACDLineIndicator, macdIndicator.getSignalIndicator)

  val macdLineUnderSignalRule =
    new UnderIndicatorRule(macdIndicator.getMACDLineIndicator, macdIndicator.getSignalIndicator)

  val closePriceOverSma = new OverIndicatorRule(closePriceIndicator.indicator, smaIndicator)
  val closePriceUnderSma = new UnderIndicatorRule(closePriceIndicator.indicator, smaIndicator)

  val plusDIIndicator = new PlusDIIndicator(series, adxBarCount)
  val minusDIIndicator = new MinusDIIndicator(series, adxBarCount)

  val plusDICrossedUpMinusDI = new CrossedUpIndicatorRule(plusDIIndicator, minusDIIndicator)
  val plusDICrossedDownMinusDI = new CrossedDownIndicatorRule(plusDIIndicator, minusDIIndicator)

  val shortEntryRule: Rule =
//    plusDICrossedDownMinusDI.and(closePriceUnderSma).and(adxSlopeDiffOverOne) //2020-12-01T11:00 @  19325.5100
//    stochUnder80Rule.and(adxOver10Rule).and(stochOver40Rule.or(adxOver20Rule)).and(closePriceUnderSma).and(macdLineUnderSignalRule) //.and(adxSlopeDiffOverOne) //2020-12-01T11:00 @  19325.5100
    macdLineCrossDownSignalRule.and(plusDICrossedDownMinusDI)

  //  val shortExitRule: Rule = adxOver20Rule.and(overSoldRule.or(plusDICrossedUpMinusDI)).and(closePriceUnderSma)
  //  val shortExitRule: Rule = overSoldRule.or(plusDICrossedUpMinusDI.and(closePriceOverSma))//.or(macdLineCrossUpSignalRule)
//  val shortExitRule: Rule = overSoldRule.or(closePriceUnderSma.and(adxSlopeDiffUnderMinusOne.or(macdLineCrossUpSignalRule)))
//  val shortExitRule: Rule = plusDICrossedUpMinusDI.or(closePriceUnderSma.and(overSoldRule)) //.or(adxSlopeDiffUnderMinusOne)))
  val shortExitRule: Rule = overSoldRule.or(plusDICrossedUpMinusDI) //.or(adxSlopeDiffUnderMinusOne)))

  private def updateADXCrossedState(crossesStatus: Option[ADXCrossesState] = None): Unit = {

    val cs = crossesStatus.fold {
      if (plusDICrossedUpMinusDI.isSatisfied(getLastIndex))
        PlusDICrossedUpMinusDI
      else if (plusDICrossedDownMinusDI.isSatisfied(getLastIndex)) {
        PlusDICrossedDownMinusDI
      } else
        strategyInternalState.adxCrossesState
    } {
      identity
    }

    strategyInternalState = strategyInternalState.copy(adxCrossesState = cs)
  }

  def evaluate(backTestStatus: BackTestStatus, currentPrice: BigDecimal, timestamp: Long): BackTestStatus = {
    updateADXCrossedState()

    backTestStatus.currentTrade match {

      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && currentPrice > trade.entryPrice * (1 + defaultStopLoss) => {
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        updateADXCrossedState(Some(Unknown))
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)
      }

      //Close Short position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && shortExitRule.isSatisfied(
            getLastIndex
          ) => {
        updateADXCrossedState(Some(Unknown))
        val description =
          if (overSoldRule.isSatisfied(getLastIndex))
            "OverSold"
//          else if (adxSlopeDiffUnderMinusOne.isSatisfied(getLastIndex))
//            "SlopeDiffUnderMinusOne"
          else if (plusDICrossedUpMinusDI.isSatisfied(getLastIndex))
            "plusDICrossedUpMinusDI"
          else "Not listed"
        sell(backTestStatus, currentPrice, timestamp, description = description)
      }

      //Close Short position by taking profit
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && currentPrice <= (trade.entryPrice - (trade.entryPrice * takeProfit)) => {
        updateADXCrossedState(Some(Unknown))
        sell(backTestStatus, currentPrice, timestamp, description = "Take profit")
      }

      //Open Short position
//      case None if series.getBarCount >= barCount && strategyInternalState.adxCrossesState == PlusDICrossedDownMinusDI && shortEntryRule.isSatisfied(getLastIndex) => {
      case None if series.getBarCount >= barCount && shortEntryRule.isSatisfied(getLastIndex) => {
        updateADXCrossedState(Some(Unknown))
        buy(backTestStatus, currentPrice, timestamp, position = Position.Short)
      }

      case _ => backTestStatus
    }
  }

  private def getLastIndex = series.getEndIndex
}

object ADXShortOptimizedStrategy {

  case class StrategyInternalState(adxCrossesState: ADXCrossesState = Unknown)

  sealed trait ADXCrossesState extends EnumEntry

  object ADXCrossesState extends Enum[ADXCrossesState] {
    val values = findValues

    case object PlusDICrossedUpMinusDI extends ADXCrossesState

    case object PlusDICrossedDownMinusDI extends ADXCrossesState

    case object Unknown extends ADXCrossesState

  }

}
