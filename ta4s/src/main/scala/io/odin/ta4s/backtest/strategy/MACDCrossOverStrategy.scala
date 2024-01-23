package io.odin.ta4s.backtest.strategy

import io.odin.binance.client.model.Candlestick
import io.odin.common.Utils
import io.odin.ta4s.backtest._
import io.odin.ta4s.indicators.OdinMACDHistogramIndicator

object MACDCrossOverStrategy extends BackTestStrategy {

  def evaluate(
    backTestStatus: BackTestStatus,
    macd: OdinMACDHistogramIndicator[Candlestick],
    currentPrice: BigDecimal,
    timestamp: Long
  ): BackTestStatus = {
    backTestStatus.currentTrade match {
      case Some(trade) if timestamp > trade.entryTimestamp && currentPrice < trade.entryPrice * (1 - defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, true)
      case Some(trade) if timestamp > trade.entryTimestamp && macd.crossedDown =>
        sell(backTestStatus, currentPrice, timestamp)
      case None if macd.crossedUp =>
        buy(backTestStatus, currentPrice, timestamp)
      case _ => backTestStatus
    }
  }
}
