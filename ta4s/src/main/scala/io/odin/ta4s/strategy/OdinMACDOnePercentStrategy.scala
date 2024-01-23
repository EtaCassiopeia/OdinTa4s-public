package io.odin.ta4s.strategy

import io.odin.ta4s.domain.{Position, Trade}
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.indicators
import org.ta4j.core.indicators.helpers.{HighPriceIndicator, LowPriceIndicator}
import org.ta4j.core.indicators.{
  EMAIndicator,
  SMAIndicator,
  StochasticOscillatorDIndicator,
  StochasticOscillatorKIndicator
}
import org.ta4j.core.trading.rules.{
  OdinCrossedDownIndicatorRule,
  OdinCrossedUpIndicatorRule,
  OverIndicatorRule,
  UnderIndicatorRule
}

/**
  *  Default Leverage: 8
  *  StopLoss: 0.05
  *  Capital: 154350.0
  *  Overall profit: 143730.0
  *  Profit rate : 1437.33
  *  Trade count: 52
  *  Success rate: 92
  */

class OdinMACDOnePercentStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends Strategy {

  private val defaultGainProfit = BigDecimal(1) * 0.01

  private val closePrice = closePriceIndicator.indicator
  private val series = closePrice.getBarSeries

  private val exponentialMovingAverage = new EMAIndicator(closePrice, barCount)
  private val simpleMovingAverage = new SMAIndicator(closePrice, barCount)

  private val stochBarCount = 14

  private val stochasticOscillatorKIndicator = new StochasticOscillatorKIndicator(
    closePrice,
    stochBarCount,
    new HighPriceIndicator(series),
    new LowPriceIndicator(series)
  )

  private val stochasticOscillatorDIndicator = new StochasticOscillatorDIndicator(stochasticOscillatorKIndicator)

  private val stochKcrossDownD =
    new OdinCrossedDownIndicatorRule(stochasticOscillatorKIndicator, stochasticOscillatorDIndicator)
  private val stochKcrossUpD =
    new OdinCrossedUpIndicatorRule(stochasticOscillatorKIndicator, stochasticOscillatorDIndicator)

  private val shortBarCount: Int = 12
  private val longBarCount: Int = 26
  private val signalBarCount: Int = 9

  private val macdIndicator =
    new indicators.OdinMACDIndicator(closePrice, shortBarCount, longBarCount, signalBarCount)

  private val macdLineIndicator = macdIndicator.getMACDLineIndicator
  private val macdSignalIndicator = macdIndicator.getSignalIndicator

  private val macdLineCrossedUpSignalRule = new OdinCrossedUpIndicatorRule(macdLineIndicator, macdSignalIndicator)
  private val macdLineCrossedDownSignalRule = new OdinCrossedDownIndicatorRule(macdLineIndicator, macdSignalIndicator)

  private val priceOverMovingAverageRule = new OverIndicatorRule(closePrice, simpleMovingAverage)
  private val priceUnderMovingAverageRule = new UnderIndicatorRule(closePrice, simpleMovingAverage)

  private val longEntryRule = macdLineCrossedUpSignalRule.and(priceOverMovingAverageRule).and(stochKcrossUpD)

  private val shortEntryRule = macdLineCrossedDownSignalRule.and(priceUnderMovingAverageRule)

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult = {
    currentTrade match {
      //Enter long position
      case None if series.getBarCount == barCount && longEntryRule.isSatisfied(endIndex) =>
        OpenPosition(strategyName, Position.Long, price, timestamp)

      //Enter short position
//      case None if series.getBarCount == barCount && shortEntryRule.isSatisfied(endIndex) =>
//        OpenPosition(strategyName, Position.Short, price, timestamp)

      //Close long position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && price >= trade.entryPrice * (1 + defaultGainProfit) =>
        ClosePosition(strategyName, Position.Long, price, timestamp)

      //Close short position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && price <= trade.entryPrice * (1 - defaultGainProfit) =>
        ClosePosition(strategyName, Position.Short, price, timestamp)

      case _ =>
        KeepCurrentSate
    }
  }

  private def endIndex = series.getEndIndex

}
