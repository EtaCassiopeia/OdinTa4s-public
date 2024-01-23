package io.odin.ta4s.backtest.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator, OdinOpenPriceIndicator}
import org.ta4j.core.Rule
import org.ta4j.core.indicators.adx.ADXIndicator
import org.ta4j.core.indicators.{EMAIndicator, MACDIndicator, OdinMACDIndicator, StochasticOscillatorKIndicator}
import org.ta4j.core.trading.rules.{
  CrossedDownIndicatorRule,
  CrossedUpIndicatorRule,
  OverIndicatorRule,
  UnderIndicatorRule
}

class LongTermMovingMomentumStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 26)
    extends BackTestStrategy {

  val series = closePriceIndicator.indicator.getBarSeries
  val closePrice = closePriceIndicator.indicator

  val macd = new OdinMACDIndicator(closePrice, 12, 26, 9)
  val macdLineCrossedDownSignalRule = new CrossedDownIndicatorRule(macd.getMACDLineIndicator, macd.getSignalIndicator)
  val macdLineCrossedUpSignalRule = new CrossedUpIndicatorRule(macd.getMACDLineIndicator, macd.getSignalIndicator)

  val adxBarCount = 14
  val adxIndicator = new ADXIndicator(series, adxBarCount)
  val adxOver20Rule = new OverIndicatorRule(adxIndicator, 20)

  val stochasticOscillK = new StochasticOscillatorKIndicator(series, 14)
  val overBoughtRule = new UnderIndicatorRule(stochasticOscillK, 20)

  // The bias is bullish when the shorter-moving average moves above the longer
  // moving average.
  // The bias is bearish when the shorter-moving average moves below the longer
  // moving average.
  val shortEma = new EMAIndicator(closePrice, 7)
  val longEma = new EMAIndicator(closePrice, 25)
  val wayLongEma = new EMAIndicator(closePrice, 99)

  // Short Exit rule
  val shortExitRule: Rule = overBoughtRule.or(
    adxOver20Rule
      .and(macdLineCrossedUpSignalRule)
  )
//  .and(new OverIndicatorRule(shortEma, wayLongEma))
//    .and(new CrossedUpIndicatorRule(longEma, wayLongEma)))
//    .and(new CrossedUpIndicatorRule(shortEma,longEma)))

  // Short Entry rule
  val shortEntryRule: Rule = adxOver20Rule
    .and(macdLineCrossedDownSignalRule)
//    .and(new UnderIndicatorRule(shortEma, wayLongEma))
//    .and(new CrossedDownIndicatorRule(longEma, wayLongEma))
//    .and(new CrossedDownIndicatorRule(shortEma,longEma))

  def evaluate(backTestStatus: BackTestStatus, currentPrice: BigDecimal, timestamp: Long): BackTestStatus = {
    backTestStatus.currentTrade match {
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && currentPrice < trade.entryPrice * (1 - defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)

      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && currentPrice > trade.entryPrice * (1 + defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)

      //Close Short position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && shortExitRule.isSatisfied(
            getLastIndex
          ) =>
        sell(
          backTestStatus,
          currentPrice,
          timestamp,
          description = if (overBoughtRule.isSatisfied(getLastIndex)) "OverBought" else ""
        )

      //Open Long position
//      case None if entryRule.isSatisfied(getLastIndex) =>
//        buy(backTestStatus, currentPrice, timestamp)

      //Open Short position
      case None if series.getBarCount >= barCount && shortEntryRule.isSatisfied(getLastIndex) =>
        buy(backTestStatus, currentPrice, timestamp, position = Position.Short)

      case _ => backTestStatus
    }
  }

  private def getLastIndex = series.getEndIndex
}
