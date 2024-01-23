package io.odin.ta4s.backtest.strategy

import io.odin.common.Utils
import io.odin.ta4s.domain.{Position, Trade, TradingParameters}
import io.odin.ta4s.backtest.BackTestStatus

trait BackTestStrategy extends TradingParameters {

  protected def buy(
    backTestStatus: BackTestStatus,
    price: BigDecimal,
    timestamp: Long,
    position: Position = Position.Long,
    description: String = ""
  ): BackTestStatus = {
    if (backTestStatus.capital <= 0)
      backTestStatus
    else {
      val amount = backTestStatus.capital / price
      val trade = Trade(
        position,
        amount,
        timestamp,
        price,
        buyDescription = if (description.isEmpty) List.empty[String] else List(description)
      )
      val fee = Utils.calculateFee(trade.amount, trade.entryPrice)
      backTestStatus.copy(
        currentTrade = Some(trade),
        overallProfit = backTestStatus.overallProfit - fee
      )
    }
  }

  protected def sell(
    backTestStatus: BackTestStatus,
    price: BigDecimal,
    timestamp: Long,
    soldOnStopLoss: Boolean = false,
    description: String = ""
  ): BackTestStatus = {
    val trade = backTestStatus.currentTrade
      .map(t =>
        t.copy(
          exitPrice = price,
          exitTimestamp = timestamp,
          soldOnStopLoss = soldOnStopLoss,
          sellDescription = t.sellDescription :+ description
        )
      )
      .get
    val fee = Utils.calculateFee(trade.amount, trade.exitPrice)
    val profitOrLoss = trade.profit - fee

    backTestStatus.copy(
      capital = backTestStatus.capital + profitOrLoss,
      currentTrade = None,
      trades = backTestStatus.trades :+ trade,
      overallProfit = backTestStatus.overallProfit + profitOrLoss
    )
  }
}
