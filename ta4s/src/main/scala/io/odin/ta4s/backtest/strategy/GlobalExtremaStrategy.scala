package io.odin.ta4s.backtest.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.Rule
import org.ta4j.core.indicators.helpers.{
  ClosePriceIndicator,
  HighPriceIndicator,
  HighestValueIndicator,
  LowPriceIndicator,
  LowestValueIndicator,
  MultiplierIndicator
}
import org.ta4j.core.trading.rules.{OverIndicatorRule, UnderIndicatorRule}

class GlobalExtremaStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends BackTestStrategy {

  val series = closePriceIndicator.indicator.getBarSeries
  private val NB_BARS_PER_DAY = 12 * 24 * 1

  val closePrices = new ClosePriceIndicator(series)

  // Getting the max price over the past day
  val maxPrices = new HighPriceIndicator(series)
  val dayMaxPrice = new HighestValueIndicator(maxPrices, NB_BARS_PER_DAY)
  // Getting the min price over the past day
  val minPrices = new LowPriceIndicator(series)
  val dayMinPrice = new LowestValueIndicator(minPrices, NB_BARS_PER_DAY)

  // Going long if the close price goes below the min price
  val downDay = new MultiplierIndicator(dayMinPrice, 1.004)
  val entryRule = new UnderIndicatorRule(closePrices, downDay)

  // Going short if the close price goes above the max price
  val upDay = new MultiplierIndicator(dayMaxPrice, 0.996)
  val exitRule = new OverIndicatorRule(closePrices, upDay)

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
