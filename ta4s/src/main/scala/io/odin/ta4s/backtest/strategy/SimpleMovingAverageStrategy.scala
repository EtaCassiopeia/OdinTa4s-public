package io.odin.ta4s.backtest.strategy

import io.odin.binance.client.model.Candlestick
import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.indicators.OdinSMAIndicator

//change(ma) > 0 ? green : change(ma) < 0 ? red : blue
object SimpleMovingAverageStrategy extends BackTestStrategy {
  def evaluate(
    backTestStatus: BackTestStatus,
    sma: OdinSMAIndicator[Candlestick],
    currentPrice: BigDecimal,
    timestamp: Long
  ): BackTestStatus = {
    backTestStatus.currentTrade match {
      case Some(trade) if timestamp > trade.entryTimestamp && currentPrice < trade.entryPrice * (1 - defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, true)
      case Some(trade) if timestamp > trade.entryTimestamp && change(sma) < 0 && change(sma, 1) >= 0 =>
        sell(backTestStatus, currentPrice, timestamp)
      case None if change(sma) > 0 && change(sma, 1) <= 0 =>
        buy(backTestStatus, currentPrice, timestamp)
      case _ => backTestStatus
    }
  }

  def change(sma: OdinSMAIndicator[Candlestick], goBack: Int = 0): Double =
    (for {
      currentValue <- sma.valueAt(sma.lastIndex - goBack)
      previousValue <- sma.valueAt(sma.lastIndex - goBack - 1)
    } yield currentValue - previousValue).fold(_ => 0, identity)
}
