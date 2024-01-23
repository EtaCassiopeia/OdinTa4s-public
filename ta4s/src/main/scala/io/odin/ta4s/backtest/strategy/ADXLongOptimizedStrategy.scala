package io.odin.ta4s.backtest.strategy

import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import io.odin.ta4s.strategy._

class ADXLongOptimizedStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends BackTestStrategy {

  private val odinADXLongOptimizedStrategy = new RiskManagementStrategy(defaultStopLoss)
    .andThen(new TakeProfitStrategy(takeProfit))
    .andThen(
      new OdinADXLongOptimizedStrategy[T](closePriceIndicator, barCount)
    )

  def evaluate(backTestStatus: BackTestStatus, currentPrice: BigDecimal, timestamp: Long): BackTestStatus = {
    val currentTrade = backTestStatus.currentTrade
    odinADXLongOptimizedStrategy.evaluate(currentTrade, timestamp, currentPrice) match {
      case OpenPosition(_, position, price, timestamp, description) =>
        buy(backTestStatus, price, timestamp, position, description)
      case ClosePosition(_, _, price, timestamp, description) =>
        sell(backTestStatus, price, timestamp, description = description)
      case KeepCurrentSate => backTestStatus
    }
  }
}
