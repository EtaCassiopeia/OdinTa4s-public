package io.odin.ta4s.backtest.strategy

import io.odin.binance.client.model.Candlestick
import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{OdinMACDHistogramIndicator, OdinStochasticOscillatorDIndicator}

object MACDStochasticReverseStrategy extends BackTestStrategy {

  def evaluate(
    backTestStatus: BackTestStatus,
    macd: OdinMACDHistogramIndicator[Candlestick],
    stoch: OdinStochasticOscillatorDIndicator[Candlestick],
    currentPrice: BigDecimal,
    timestamp: Long
  ): BackTestStatus = {

    var recentMACDCrossesState: Option[CrossedState] = None

    recentMACDCrossesState =
      if (macd.crossedUp)
        Some(CrossedState.CrossedUp)
      else if (macd.crossedDown)
        Some(CrossedState.CrossedDown)
      else recentMACDCrossesState

    backTestStatus.currentTrade match {
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && currentPrice < trade.entryPrice * (1 - defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)

      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && currentPrice > trade.entryPrice * (1 + defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)

      //Close Long position
      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Long && stoch.crossedDown =>
        recentMACDCrossesState
          .withFilter(_ == CrossedState.CrossedUp) //Get ready to sell
          .map(_ => sell(backTestStatus, currentPrice, timestamp))
          .fold(backTestStatus)(identity)

      //Close Oversold Long position
//      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Long && stoch.oversold => {
//        val intermediateState = recentMACDCrossesState
//          .withFilter(_ == CrossedState.CrossedDown)
//          .map(_ => sell(backTestStatus, currentPrice, timestamp, description = "Oversold"))
//          .fold(backTestStatus)(identity)
//
//        intermediateState
//      }

      //Open Long position
      case None if stoch.crossedUp => //&& stoch.value.map( _ <= 50).getOrElse(false) =>
        recentMACDCrossesState
          .withFilter(_ == CrossedState.CrossedDown) // Get ready to buy
          .map(_ => buy(backTestStatus, currentPrice, timestamp))
          .fold(backTestStatus)(identity)

      case _ => backTestStatus
    }
  }

}
