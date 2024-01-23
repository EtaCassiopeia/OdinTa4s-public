package io.odin.ta4s.strategy

import io.odin.common.Utils
import io.odin.ta4s.domain.{Position, Trade}

class RiskManagementStrategy(stopLoss: BigDecimal) extends Strategy {

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult = {
    currentTrade match {
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && price < trade.entryPrice * (1 - stopLoss) =>
        val description = s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $price "
        println(description)
        ClosePosition(strategyName, Position.Long, price, timestamp, description)

      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && price > trade.entryPrice * (1 + stopLoss) =>
        val description = s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $price "
        println(description)
        ClosePosition(strategyName, Position.Short, price, timestamp, description)

      case _ =>
        KeepCurrentSate
    }
  }
}
