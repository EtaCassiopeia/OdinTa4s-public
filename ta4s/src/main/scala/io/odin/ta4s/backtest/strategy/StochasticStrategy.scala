package io.odin.ta4s.backtest.strategy

import io.odin.binance.client.model.Candlestick
import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.OdinStochasticOscillatorDIndicator

object StochasticStrategy extends BackTestStrategy {

  def evaluate(
    backTestStatus: BackTestStatus,
    stoch: OdinStochasticOscillatorDIndicator[Candlestick],
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

      //Close Long position
      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Long && stoch.crossedDown =>
        sell(backTestStatus, currentPrice, timestamp)

      //Close Oversold Long position
      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Long && stoch.oversold =>
        sell(backTestStatus, currentPrice, timestamp, description = "Oversold")

      //Open Long position
      case None if stoch.crossedUp => //&& stoch.value.map( _ <= 50).getOrElse(false) =>
        buy(backTestStatus, currentPrice, timestamp)

      case _ => backTestStatus
    }
  }

}
