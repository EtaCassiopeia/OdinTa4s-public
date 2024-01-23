package io.odin.ta4s.strategy

import io.odin.ta4s.domain.{Position, Trade, TradingParameters}

sealed trait StrategyEvaluationResult

case class OpenPosition(
  strategy: String,
  position: Position,
  price: BigDecimal,
  timestamp: Long,
  description: String = ""
) extends StrategyEvaluationResult

case class ClosePosition(
  strategy: String,
  position: Position,
  price: BigDecimal,
  timestamp: Long,
  description: String = ""
) extends StrategyEvaluationResult

case object KeepCurrentSate extends StrategyEvaluationResult

trait Strategy extends TradingParameters {
  def strategyName: String = this.getClass.getSimpleName
  def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult

  def andThen(secondStrategy: Strategy): CombinedStrategy = new CombinedStrategy(this, secondStrategy)
}

class CombinedStrategy private[strategy] (firstStrategy: Strategy, secondStrategy: Strategy) extends Strategy {

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult =
    firstStrategy.evaluate(currentTrade, timestamp, price) match {
      case KeepCurrentSate => secondStrategy.evaluate(currentTrade, timestamp, price)
      case other           => other
    }
}
