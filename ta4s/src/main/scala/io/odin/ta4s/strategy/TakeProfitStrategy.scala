package io.odin.ta4s.strategy

import io.odin.ta4s.domain.{Position, Trade}

class TakeProfitStrategy(defaultGainProfit: BigDecimal) extends Strategy {

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult = {
    currentTrade match {

      //Close Long position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && (price >= trade.entryPrice * (1 + defaultGainProfit)) =>
        ClosePosition(strategyName, Position.Long, price, timestamp)

      //Close Short position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && (price <= trade.entryPrice * (1 - defaultGainProfit)) =>
        ClosePosition(strategyName, Position.Short, price, timestamp)

      case _ =>
        KeepCurrentSate
    }
  }
}
