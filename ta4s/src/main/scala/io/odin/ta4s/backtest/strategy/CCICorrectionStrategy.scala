package io.odin.ta4s.backtest.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.Rule
import org.ta4j.core.indicators.CCIIndicator
import org.ta4j.core.num.Num
import org.ta4j.core.trading.rules.{OverIndicatorRule, UnderIndicatorRule}

class CCICorrectionStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 200)
    extends BackTestStrategy {
  val series = closePriceIndicator.indicator.getBarSeries

  val longCci = new CCIIndicator(series, barCount)
  val shortCci = new CCIIndicator(series, 5)
  val plus100: Num = series.numOf(100)
  val minus100: Num = series.numOf(-100)

  val entryRule: Rule = new OverIndicatorRule(longCci, plus100) // Bull trend
    .and(new UnderIndicatorRule(shortCci, minus100)) // Signal

  val exitRule: Rule = new UnderIndicatorRule(longCci, minus100) // Bear trend
    .and(new OverIndicatorRule(shortCci, plus100))

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
      case None if series.getBarCount >= 200 && entryRule.isSatisfied(getLastIndex) =>
        buy(backTestStatus, currentPrice, timestamp)

      case _ => backTestStatus
    }
  }

  private def getLastIndex = series.getEndIndex

}
