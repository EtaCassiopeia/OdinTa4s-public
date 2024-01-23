package io.odin.ta4s.backtest.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.Rule
import org.ta4j.core.indicators.{EMAIndicator, MACDIndicator, StochasticOscillatorKIndicator}
import org.ta4j.core.trading.rules.{
  CrossedDownIndicatorRule,
  CrossedUpIndicatorRule,
  OverIndicatorRule,
  UnderIndicatorRule
}

class MovingMomentumStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends BackTestStrategy {

  val series = closePriceIndicator.indicator.getBarSeries
  val closePrice = closePriceIndicator.indicator

  // The bias is bullish when the shorter-moving average moves above the longer
  // moving average.
  // The bias is bearish when the shorter-moving average moves below the longer
  // moving average.
  val shortEma = new EMAIndicator(closePrice, 9)
  val longEma = new EMAIndicator(closePrice, 26)

  val stochasticOscillK = new StochasticOscillatorKIndicator(series, 14)

  val macd = new MACDIndicator(closePrice, 9, 26)
  val emaMacd = new EMAIndicator(macd, 18)

  // Entry rule
  val entryRule: Rule = new OverIndicatorRule(shortEma, longEma) // Trend
    .and(new CrossedDownIndicatorRule(stochasticOscillK, 20)) // Signal 1
    .and(new OverIndicatorRule(macd, emaMacd)) // Signal 2

  // Exit rule
  val exitRule: Rule = new UnderIndicatorRule(shortEma, longEma)
    .and(new CrossedUpIndicatorRule(stochasticOscillK, 20))
    .and(new UnderIndicatorRule(macd, emaMacd))

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

      //Close Long position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && exitRule.isSatisfied(
            getLastIndex
          ) =>
        sell(backTestStatus, currentPrice, timestamp)

      //Open Long position
      case None if entryRule.isSatisfied(getLastIndex) =>
        buy(backTestStatus, currentPrice, timestamp)

      case _ => backTestStatus
    }
  }

  private def getLastIndex = series.getEndIndex
}
