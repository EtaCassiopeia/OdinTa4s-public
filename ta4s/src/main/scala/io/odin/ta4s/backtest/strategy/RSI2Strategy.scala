package io.odin.ta4s.backtest.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.Rule
import org.ta4j.core.indicators.{RSIIndicator, SMAIndicator}
import org.ta4j.core.trading.rules.{
  CrossedDownIndicatorRule,
  CrossedUpIndicatorRule,
  OverIndicatorRule,
  UnderIndicatorRule
}

class RSI2Strategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 200)
    extends BackTestStrategy {
  val series = closePriceIndicator.indicator.getBarSeries

  val closePrice = closePriceIndicator.indicator

  val shortSma = new SMAIndicator(closePrice, 5)
  val longSma = new SMAIndicator(closePrice, 200)

  // We use a 2-period RSI indicator to identify buying
  // or selling opportunities within the bigger trend.
  val rsi = new RSIIndicator(closePrice, 2)

  // Entry rule
  // The long-term trend is up when a security is above its 200-period SMA.
  val entryRule: Rule = (new OverIndicatorRule(shortSma, longSma)) // Trend
    .and(new CrossedDownIndicatorRule(rsi, 5)) // Signal 1
    .and(new OverIndicatorRule(shortSma, closePrice)) // Signal 2

  // Exit rule
  // The long-term trend is down when a security is below its 200-period SMA.
  val exitRule: Rule = new UnderIndicatorRule(shortSma, longSma)
    .and(new CrossedUpIndicatorRule(rsi, 95))
    .and(new UnderIndicatorRule(shortSma, closePrice))

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
