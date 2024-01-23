package io.odin.ta4s.backtest.strategy

import io.odin.binance.client.model.Candlestick
import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{OdinMACDHistogramIndicator, OdinStochasticOscillatorDIndicator}

object MACDStochasticStrategy extends BackTestStrategy {

  def evaluate(
    backTestStatus: BackTestStatus,
    macd: OdinMACDHistogramIndicator[Candlestick],
    stoch: OdinStochasticOscillatorDIndicator[Candlestick],
    currentPrice: BigDecimal,
    timestamp: Long
  ): BackTestStatus = {

//    import io.odin.ta4s.IndicatorUtils._
//    println(s"[${Utils.timestampToLocalDateTime(timestamp)}] stochK: ${value(stoch.stochasticOscillatorKIndicator)}, stochD: ${value(stoch.indicator)}, macdLine: ${value(macd.getMACDIndicator)}, macdSignal: ${value(macd.getSignalIndicator)}")
//    println(s"${macd.crossedUp}, ${macd.crossedDown}, ${stoch.crossedUp}, ${stoch.crossedDown}, ${stoch.value.map(_ <= 50).getOrElse(false)}")

    var recentStochCrossesState: Option[CrossedState] = None
    var stochDetails: String = ""

    recentStochCrossesState = if (stoch.crossedUp) {
      stochDetails = stoch.details
      Some(CrossedState.CrossedUp)
    } else if (stoch.crossedDown) {
      stochDetails = stoch.details
      Some(CrossedState.CrossedDown)
    } else recentStochCrossesState

//    println(s"[${Utils.timestampToLocalDateTime(timestamp)}] ${recentStochCrossesState}")

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
      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Long && macd.crossedDown =>
        //recentStochCrossesState.withFilter(_ == CrossedState.CrossedUp).map(_ =>sell(backTestStatus, currentPrice, timestamp)).fold(backTestStatus)(identity)
        recentStochCrossesState
          .withFilter(_ == CrossedState.CrossedDown)
          .map(_ => sell(backTestStatus, currentPrice, timestamp))
          .fold(backTestStatus)(identity)

      //Close Oversold Long position
      /*case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Long && stoch.overbought => {
        val intermediateState = recentStochCrossesState
          .withFilter(_ == CrossedState.CrossedDown)
          .map(_ => sell(backTestStatus, currentPrice, timestamp, description = "Oversold"))
          .fold(backTestStatus)(identity)

        //open short immediately
//        recentStochCrossesState
//          .withFilter(_ == CrossedState.CrossedDown)
//          .map(_ => buy(intermediateState, currentPrice, timestamp, Position.Short))
//          .fold(intermediateState)(identity)

        intermediateState
      }*/

      //Close Long position forced by Stoch
//      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Long =>
//        recentStochCrossesState
//          .withFilter(_ == CrossedState.CrossedDown)
//          .map(_ => sell(backTestStatus, currentPrice, timestamp, description = "Forced by Stochastic"))
//          .fold(backTestStatus)(identity)

      //Close Short position
      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Short && macd.crossedUp =>
        recentStochCrossesState
          .withFilter(_ == CrossedState.CrossedUp)
          .map(_ => sell(backTestStatus, currentPrice, timestamp))
          .fold(backTestStatus)(identity)

      //Open Long position
      case None if macd.crossedUp && stoch.value.map(_ <= 50).getOrElse(false) => {

//        println(Utils.timestampToLocalDateTime(timestamp))
//        println(stochDetails)

        recentStochCrossesState
          .withFilter(_ == CrossedState.CrossedUp)
          .map(_ => buy(backTestStatus, currentPrice, timestamp))
          .fold(backTestStatus)(identity)
      }

      //Open Short position
//      case None if macd.crossedDown && stoch.value.map( _ <= 50).getOrElse(false)=>
//        recentStochCrossesState
//          .withFilter(_ == CrossedState.CrossedDown)
//          .map(_ => buy(backTestStatus, currentPrice, timestamp, Position.Short))
//          .fold(backTestStatus)(identity)

      case _ => backTestStatus
    }
  }

}
