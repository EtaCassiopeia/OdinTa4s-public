package io.odin.ta4s.backtest.strategy

import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import io.odin.ta4s.strategy._

class ChaikinSqueezeMomentumStrategy[T: BarBuilder](
  closePriceIndicator: OdinClosePriceIndicator[T],
  barCount: Int = 200
) extends BackTestStrategy {

  private val odinChaikinSqueezingMomentumStrategy = new RiskManagementStrategy(defaultStopLoss)
    .andThen(new TakeProfitStrategy(takeProfit))
    .andThen(
      new OdinChaikinSqueezingMomentumStrategy[T](closePriceIndicator, barCount)
    )

  def evaluate(
    backTestStatus: BackTestStatus,
    currentPrice: BigDecimal,
    timestamp: Long
  ): BackTestStatus = {

    val currentTrade = backTestStatus.currentTrade
    odinChaikinSqueezingMomentumStrategy.evaluate(currentTrade, timestamp, currentPrice) match {
      case OpenPosition(_, position, price, timestamp, description) =>
        buy(backTestStatus, price, timestamp, position, description)
      case ClosePosition(_, _, price, timestamp, description) =>
        sell(backTestStatus, price, timestamp, description = description)
      case KeepCurrentSate => backTestStatus
    }
  }
}
