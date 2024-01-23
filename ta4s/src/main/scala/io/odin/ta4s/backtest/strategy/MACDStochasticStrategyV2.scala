package io.odin.ta4s.backtest.strategy

import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import io.odin.ta4s.strategy._

class MACDStochasticStrategyV2[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T])
    extends BackTestStrategy {

  val odinMACDStochasticStrategy = new OdinMACDStochasticStrategy[T](closePriceIndicator)
  val riskManagementStrategy = new RiskManagementStrategy(defaultStopLoss)

  def evaluate(
    backTestStatus: BackTestStatus,
    currentPrice: BigDecimal,
    timestamp: Long
  ): BackTestStatus = {

    val currentTrade = backTestStatus.currentTrade
    val riskManagementStrategyResult: StrategyEvaluationResult =
      riskManagementStrategy.evaluate(currentTrade, timestamp, currentPrice)

    riskManagementStrategyResult match {
      case ClosePosition(_, _, price, timestamp, description) =>
        sell(backTestStatus, price, timestamp, soldOnStopLoss = true, description = description)
      case _ =>
        odinMACDStochasticStrategy.evaluate(currentTrade, timestamp, currentPrice) match {
          case OpenPosition(_, _, price, timestamp, description) =>
            buy(backTestStatus, price, timestamp, Position.Long, description)
          case ClosePosition(_, _, price, timestamp, description) =>
            sell(backTestStatus, price, timestamp, description = description)
          case KeepCurrentSate => backTestStatus
        }
    }

  }

}
