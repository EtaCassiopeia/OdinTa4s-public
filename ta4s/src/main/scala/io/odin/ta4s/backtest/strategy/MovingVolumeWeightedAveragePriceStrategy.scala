package io.odin.ta4s.backtest.strategy

import io.odin.binance.client.model.Candlestick
import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.backtest.strategy.SimpleMovingAverageStrategy.sell
import io.odin.ta4s.indicators.{MarketTrend, OdinMVWAPIndicator, OdinParabolicSarIndicator, OdinSMAIndicator}

//change(ma) > 0 ? green : change(ma) < 0 ? red : blue
object MovingVolumeWeightedAveragePriceStrategy extends BackTestStrategy {
  def evaluate(
    backTestStatus: BackTestStatus,
    mvwap: OdinMVWAPIndicator[Candlestick],
//    trend: OdinParabolicSarIndicator[Candlestick],
    currentPrice: BigDecimal,
    timestamp: Long
  ): BackTestStatus = {
//    val marketTrend = trend.marketTrend
    backTestStatus.currentTrade match {
      case Some(trade) if timestamp > trade.entryTimestamp && currentPrice < trade.entryPrice * (1 - defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, true)
      case Some(trade)
          if timestamp > trade.entryTimestamp && change(mvwap) < -2 && change(mvwap, 1) >= 0 => //-2 the best ever result
        sell(backTestStatus, currentPrice, timestamp)
      case None if change(mvwap) > 0 && change(mvwap, 1) <= 0 =>
        buy(backTestStatus, currentPrice, timestamp)
//      case None if change(mvwap) > 0 && change(mvwap, 1) <= 0 =>
//        println(s"Was supposed to buy but market is ${marketTrend} ${Utils.timestampToLocalDateTime(timestamp)}")
//        backTestStatus
      case _ => backTestStatus
    }
  }

  def change(mvwap: OdinMVWAPIndicator[Candlestick], goBack: Int = 0): Double =
    (for {
      currentValue <- mvwap.valueAt(mvwap.lastIndex - goBack)
      previousValue <- mvwap.valueAt(mvwap.lastIndex - goBack - 1)
    } yield currentValue - previousValue).fold(_ => 0, identity)
}
