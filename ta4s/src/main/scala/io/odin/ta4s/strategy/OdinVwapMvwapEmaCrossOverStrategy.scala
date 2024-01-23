package io.odin.ta4s.strategy

import io.odin.common.Utils
import io.odin.ta4s.domain.{Position, Signal, Trade}
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.indicators.helpers.{HighPriceIndicator, LowPriceIndicator}
import org.ta4j.core.indicators.{
  EMAIndicator,
  RSIIndicator,
  SMAIndicator,
  StochasticOscillatorDIndicator,
  StochasticOscillatorKIndicator
}
import org.ta4j.core.indicators.volume.VWAPIndicator
import org.ta4j.core.trading.rules.{
  OdinCrossedDownIndicatorRule,
  OdinCrossedUpIndicatorRule,
  OverIndicatorRule,
  OverOrEqualIndicatorRule,
  UnderIndicatorRule,
  UnderOrEqualIndicatorRule
}

//VWAP/MVWAP/EMA CROSSOVER by derricklaflame
class OdinVwapMvwapEmaCrossOverStrategy[T: BarBuilder](
  closePriceIndicator: OdinClosePriceIndicator[T],
  barCount: Int = 50
) extends Strategy {

  private val defaultGainProfit = BigDecimal(1) * 0.01

  private val closePrice = closePriceIndicator.indicator
  private val series = closePrice.getBarSeries

  //VWAP Length
  private val vwapLength = 1
  private val ema1 = new EMAIndicator(closePrice, 7)
  private val ema2 = new EMAIndicator(closePrice, 25)

  //RSI Limit (RISKY)
  private val rsiLimit = 65
  //RSI Minimum (WAIT FOR DIP)
  private val rsiMinimum = 51

  //MVWAP
  //MVWAP Length
  private val avLength = 21
  private val vwap = new VWAPIndicator(series, 23) //TODO change barCount: 22,24,25,30 | 23
//  val vwaps: Seq[(VWAPIndicator, Int)] = (1 to barCount).map(i => new VWAPIndicator(series, i)).zipWithIndex
  private val mvwap = new EMAIndicator(vwap, avLength)

  // VWAP
  private val cvwap = new EMAIndicator(vwap, vwapLength)

  private val rsi = new RSIIndicator(closePrice, 14)
  private val simpleMovingAverage = new SMAIndicator(closePrice, barCount)
  private val priceOverMovingAverageRule = new OverIndicatorRule(closePrice, simpleMovingAverage)
  private val priceUnderMovingAverageRule = new UnderIndicatorRule(closePrice, simpleMovingAverage)

  private val rsiOver40Rule = new OverIndicatorRule(rsi, 40)

  private val ema1OverMvwapRule = new OverOrEqualIndicatorRule(ema1, mvwap)
  private val ema2OverMvwapRule = new OverOrEqualIndicatorRule(ema2, mvwap)

  private val cvwapOverMvwapRule = new OverOrEqualIndicatorRule(cvwap, mvwap)

  private val crossRule = ema1OverMvwapRule.and(ema2OverMvwapRule)
  private val crosssUpRule = crossRule.and(cvwapOverMvwapRule)

  private val buyRule = crosssUpRule

  private val riskyRule = crosssUpRule.and(new OverIndicatorRule(rsi, rsiLimit))
  private val buyDipRule = crosssUpRule.and(new UnderIndicatorRule(rsi, rsiMinimum))
  private val goodBuy = buyRule.and(new UnderIndicatorRule(rsi, rsiLimit))
  private val greatBuy = goodBuy.and(new OverIndicatorRule(rsi, rsiMinimum))

  private val stochBarCount = 14

  private val stochasticOscillatorKIndicator = new StochasticOscillatorKIndicator(
    closePrice,
    stochBarCount,
    new HighPriceIndicator(series),
    new LowPriceIndicator(series)
  )
  private val stochasticOscillatorDIndicator = new StochasticOscillatorDIndicator(stochasticOscillatorKIndicator)

  private val stochDUnder50Rule = new UnderOrEqualIndicatorRule(stochasticOscillatorDIndicator, 50)
  private val stochDOver50Rule = new OverOrEqualIndicatorRule(stochasticOscillatorDIndicator, 50)

  private val stochKcrossedDownD =
    new UnderIndicatorRule(stochasticOscillatorKIndicator, stochasticOscillatorDIndicator)
  private val stochKcrossedUpD =
    new OverIndicatorRule(stochasticOscillatorKIndicator, stochasticOscillatorDIndicator)

  private var previousSignal: Signal = Signal.Unknown

  private def description: String = {
    s"""
       |riskyRule: ${riskyRule.isSatisfied(endIndex)}
       |buyDipRule: ${buyDipRule.isSatisfied(endIndex)}
       |goodBuy: ${goodBuy.isSatisfied(endIndex)}
       |greatBuy: ${greatBuy.isSatisfied(endIndex)}
       |vwap: ${vwap.getValue(vwap.getBarSeries.getEndIndex)}
       |rsi: ${rsi.getValue(rsi.getBarSeries.getEndIndex)}
       |sma: ${simpleMovingAverage.getValue(simpleMovingAverage.getBarSeries.getEndIndex)}
       |stochasticOscillatorKIndicator: ${stochasticOscillatorKIndicator.getValue(
      stochasticOscillatorKIndicator.getBarSeries.getEndIndex
    )}
       |stochasticOscillatorDIndicator: ${stochasticOscillatorDIndicator.getValue(
      stochasticOscillatorDIndicator.getBarSeries.getEndIndex
    )}
       |""".stripMargin
  }

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult = {
    val currentSignal = if (buyRule.isSatisfied(endIndex)) Signal.LongSignal else Signal.ShortSignal
//    vwaps.foreach{
//      case (v,i) => if(v.getValue(v.getBarSeries.getEndIndex).isGreaterThanOrEqual( v.numOf( 230)) && v.getValue(v.getBarSeries.getEndIndex).isLessThanOrEqual( v.numOf( 23100))) println(s"### ${Utils.timestampToLocalDateTime(timestamp)} - ${v.getValue(v.getBarSeries.getEndIndex)} $i")
//    }

    val evaluationResult = currentTrade match {
      //Close Long position
//      case Some(trade) if trade.position == Position.Long && currentSignal != previousSignal =>
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && (price >= trade.entryPrice * (1 + defaultGainProfit) || currentSignal != previousSignal) =>
        ClosePosition(strategyName, Position.Long, price, timestamp)
      //Close Short position
//      case Some(trade) if trade.position == Position.Short && currentSignal != previousSignal =>
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && (price <= trade.entryPrice * (1 - defaultGainProfit) || currentSignal != previousSignal) =>
        ClosePosition(strategyName, Position.Short, price, timestamp)
      //Open Long position
      case None
          if previousSignal != Signal.Unknown && currentSignal != previousSignal && currentSignal == Signal.LongSignal && rsiOver40Rule
            .isSatisfied(endIndex) && stochKcrossedUpD.isSatisfied(endIndex) =>
        OpenPosition(strategyName, Position.Long, price, timestamp, description)
      //Open Short position
//      case None
//          if previousSignal != Signal.Unknown && currentSignal != previousSignal && currentSignal == Signal.ShortSignal && rsiOver40Rule
//            .isSatisfied(endIndex) && stochKcrossedDownD.isSatisfied(endIndex) =>
//        OpenPosition(strategyName, Position.Short, price, timestamp, description)
      case _ => KeepCurrentSate
    }

//    if(previousSignal != currentSignal) println(s"[${Utils.timestampToLocalDateTime(timestamp)}] position changed from $previousSignal to $currentSignal")
    previousSignal = currentSignal

    evaluationResult
  }

  private def endIndex = series.getEndIndex
}
