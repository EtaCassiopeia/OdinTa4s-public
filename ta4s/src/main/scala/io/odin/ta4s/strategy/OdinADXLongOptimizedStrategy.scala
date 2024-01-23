package io.odin.ta4s.strategy

import io.odin.common.Utils
import io.odin.ta4s.domain.{Position, Trade}
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.Rule
import org.ta4j.core.indicators.{
  EMAIndicator,
  OdinMACDIndicator,
  SMAIndicator,
  StochasticOscillatorDIndicator,
  StochasticOscillatorKIndicator,
  ValueDifferenceIndicator
}
import org.ta4j.core.indicators.adx.{ADXIndicator, MinusDIIndicator, PlusDIIndicator}
import org.ta4j.core.indicators.helpers.{DifferencePercentage, VolumeIndicator}
import org.ta4j.core.indicators.volume.{MVWAPIndicator, VWAPIndicator}
import org.ta4j.core.trading.rules.{
  CrossedDownIndicatorRule,
  CrossedUpIndicatorRule,
  OverIndicatorRule,
  UnderIndicatorRule
}

class OdinADXLongOptimizedStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends Strategy {

  val series = closePriceIndicator.indicator.getBarSeries
  val closePrice = closePriceIndicator.indicator

  val volumeIndicator = new VolumeIndicator(series, barCount)
  val volumeDifferencePercentageIndicator = new DifferencePercentage(volumeIndicator)
  //  val volumeDifferencePercentageChangeIndicator = new ValueDifferenceIndicator(volumeDifferencePercentageIndicator)
  val negativeVolumeDifferencePercentage = new UnderIndicatorRule(volumeDifferencePercentageIndicator, 0)

  val smaIndicator = new SMAIndicator(closePriceIndicator.indicator, 50)
  val sma7Indicator = new SMAIndicator(closePriceIndicator.indicator, 7)
  val emaIndicator = new EMAIndicator(closePriceIndicator.indicator, barCount)
  val vwapIndicator = new VWAPIndicator(series, barCount)
  val mvwapIndicator = new MVWAPIndicator(vwapIndicator, barCount)

  val adxBarCount = 14
  val adxIndicator = new ADXIndicator(series, adxBarCount)
  //  val adxIndicator = new OdinADXIndicator(series, adxBarCount)
  val adxOver20Rule = new OverIndicatorRule(adxIndicator, 20)

  val macdIndicator = new OdinMACDIndicator(closePrice)
  val macdLineOverSignalRule =
    new OverIndicatorRule(macdIndicator.getMACDLineIndicator, macdIndicator.getSignalIndicator)

  val adxValueDifferenceIndicator = new ValueDifferenceIndicator(adxIndicator)
  val adxGrowthIsNegative = new UnderIndicatorRule(adxValueDifferenceIndicator, -2)

  val stochasticOscillK = new StochasticOscillatorKIndicator(series, 14)
  val stochasticOscillD = new StochasticOscillatorDIndicator(stochasticOscillK)
  val overSoldRule = new UnderIndicatorRule(stochasticOscillK, 20)
  val overBoughtRule = new OverIndicatorRule(stochasticOscillK, 80)
  val stochUnder80Rule = new UnderIndicatorRule(stochasticOscillK, 80)
  val stochOver50Rule = new OverIndicatorRule(stochasticOscillK, 50)
  val stochasticKOverDRule = new OverIndicatorRule(stochasticOscillK, stochasticOscillD)

  val closePriceOverSma = new OverIndicatorRule(closePriceIndicator.indicator, smaIndicator)
  val closePriceUnderSma = new UnderIndicatorRule(closePriceIndicator.indicator, smaIndicator)

  val plusDIIndicator = new PlusDIIndicator(series, adxBarCount)
  val minusDIIndicator = new MinusDIIndicator(series, adxBarCount)

  val plusDICrossedUpMinusDI = new CrossedUpIndicatorRule(plusDIIndicator, minusDIIndicator)
  val plusDICrossedDownMinusDI = new CrossedDownIndicatorRule(plusDIIndicator, minusDIIndicator)

  //val longEntryRule: Rule = adxOver20Rule.and(plusDICrossedUpMinusDI).and(closePriceOverSma).and(stochOver50Rule).and(stochUnder80Rule)
  //val longEntryRule: Rule = adxOver20Rule.and(plusDICrossedUpMinusDI).and(closePriceOverSma)//.and(stochUnder80Rule) 44%

  val longEntryRule: Rule =
    adxOver20Rule
      .and(plusDICrossedUpMinusDI)
      .and(closePriceOverSma)
  //  .and(macdLineOverSignalRule)
  // .and(stochasticKOverDRule)
  // .and(stochUnder80Rule)
  val shortEntryRule: Rule =
    adxOver20Rule.and(plusDICrossedDownMinusDI).and(closePriceUnderSma) //.and(stochasticKOverDRule)

  //  val longExitRule: Rule = adxOver20Rule.and(plusDICrossedDownMinusDI).and(closePriceUnderSma) //The first version
  val longExitRule: Rule = adxOver20Rule.and(overBoughtRule.or(plusDICrossedDownMinusDI)).and(closePriceUnderSma)
  //  val longExitRule: Rule = adxOver20Rule.and(plusDICrossedDownMinusDI) //.and(closePriceUnderSma) //The first version
  //  val longExitRule: Rule = (overSoldRule.or(plusDICrossedDownMinusDI))//.and(closePriceUnderSma)//.and(stochOver50Rule)
  //  val longExitRule: Rule =  negativeVolumeDifferencePercentage.or(adxOver20Rule.and(overSoldRule.or(plusDICrossedDownMinusDI)).and(closePriceUnderSma))//.and(stochOver50Rule)
  val shortExitRule: Rule = adxOver20Rule.and(overSoldRule.or(plusDICrossedUpMinusDI)).and(closePriceOverSma)

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult =
    currentTrade match {

      // take profit || price >= (trade.entryPrice * takeProfit) + trade.entryPrice)
      //Close Long position either by receiving a signal or taking some fixed percentage of profit
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && longExitRule.isSatisfied(
            getLastIndex
          ) =>
        ClosePosition(strategyName, Position.Long, price, timestamp)

      //Close Long position because there is another Short signal
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && shortEntryRule.isSatisfied(
            getLastIndex
          ) =>
        val description = "An opposite signal has been received"

        ClosePosition(strategyName, Position.Long, price, timestamp, description)

      //Close Short position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && shortExitRule.isSatisfied(
            getLastIndex
          ) =>
        ClosePosition(strategyName, Position.Short, price, timestamp)

      //Open Long position
      case None
          if series.getBarCount >= barCount && longEntryRule
            .isSatisfied(
              getLastIndex
            ) => //&& minusDIIndicator.getValue(minusDIIndicator.getBarSeries.getEndIndex-1).isGreaterThan( minusDIIndicator.getValue(minusDIIndicator.getBarSeries.getEndIndex)) => {
        OpenPosition(strategyName, Position.Long, price, timestamp)

//      Open Short position
//            case None if series.getBarCount >= barCount &&  shortEntryRule.isSatisfied(getLastIndex) =>
//              OpenPosition(strategyName, Position.Short, price, timestamp)

      case _ => KeepCurrentSate
    }

  private def getLastIndex = series.getEndIndex
}
