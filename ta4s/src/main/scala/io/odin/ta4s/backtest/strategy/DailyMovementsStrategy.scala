package io.odin.ta4s.backtest.strategy

import io.odin.binance.client.model.Candlestick
import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{OdinDailyHighIndicator, OdinDailyLowIndicator, OdinMVWAPIndicator}

object DailyMovementsStrategy extends BackTestStrategy {
  def evaluate(
    backTestStatus: BackTestStatus,
    dailyHigh: OdinDailyHighIndicator[Candlestick],
    dailyLow: OdinDailyLowIndicator[Candlestick],
    currentPrice: BigDecimal,
    timestamp: Long
  ): BackTestStatus = {
    backTestStatus.currentTrade match {
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && currentPrice < trade.entryPrice * (1 - defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)

      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && currentPrice > trade.entryPrice * (1 + defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)

      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && dailyHigh.change == 0 =>
        sell(backTestStatus, currentPrice, timestamp)

      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && dailyLow.change == 0 =>
        sell(backTestStatus, currentPrice, timestamp)

      case None if dailyHigh.change > 0 =>
        buy(backTestStatus, currentPrice, timestamp)

      case None if dailyLow.change < 0 =>
//        println(s"Entering short on ${dailyLow.change}")
        buy(backTestStatus, currentPrice, timestamp, position = Position.Short)
      case _ => backTestStatus
    }
  }

  def change(mvwap: OdinMVWAPIndicator[Candlestick], goBack: Int = 0): Double =
    (for {
      currentValue <- mvwap.valueAt(mvwap.lastIndex - goBack)
      previousValue <- mvwap.valueAt(mvwap.lastIndex - goBack - 1)
    } yield currentValue - previousValue).fold(_ => 0, identity)
}
