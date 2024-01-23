package io.odin.ta4s.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.strategy.CrossedState
import io.odin.ta4s.domain.{Position, Trade}
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.indicators
import org.ta4j.core.indicators.helpers.{HighPriceIndicator, LowPriceIndicator}
import org.ta4j.core.indicators.{StochasticOscillatorDIndicator, StochasticOscillatorKIndicator}
import org.ta4j.core.trading.rules.{OdinCrossedDownIndicatorRule, OdinCrossedUpIndicatorRule, UnderOrEqualIndicatorRule}

class OdinMACDStochasticStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T]) extends Strategy {

  private val closePrice = closePriceIndicator.indicator
  private val series = closePrice.getBarSeries

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

  private val stochDUnder50Rule = new UnderOrEqualIndicatorRule(stochasticOscillatorDIndicator, 50)

  private val shortBarCount: Int = 12
  private val longBarCount: Int = 26
  private val signalBarCount: Int = 9

  private val macdIndicator =
    new indicators.OdinMACDIndicator(closePrice, shortBarCount, longBarCount, signalBarCount)

  private val macdLineIndicator = macdIndicator.getMACDLineIndicator
  private val macdSignalIndicator = macdIndicator.getSignalIndicator

  private val macdLineCrossedUpSignalRule = new OdinCrossedUpIndicatorRule(macdLineIndicator, macdSignalIndicator)
  private val macdLineCrossedDownSignalRule = new OdinCrossedDownIndicatorRule(macdLineIndicator, macdSignalIndicator)

  private val longEntryRule = macdLineCrossedUpSignalRule.and(stochKcrossUpD).and(stochDUnder50Rule)
  private val longExitRule = macdLineCrossedDownSignalRule.and(stochKcrossDownD)

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult = {

//    import io.odin.ta4s.IndicatorUtils._
//
//    println(s"[${Utils.timestampToLocalDateTime(timestamp)}] stochK: ${value(stochasticOscillatorKIndicator)}, stochD: ${value(stochasticOscillatorDIndicator)}, macdLine: ${value(macdLineIndicator)}, macdSignal: ${value(macdSignalIndicator)}")
//    println(s"${macdLineCrossedUpSignalRule.isSatisfied(endIndex)}, ${macdLineCrossedDownSignalRule.isSatisfied(endIndex)}, ${stochKcrossUpD.isSatisfied(endIndex)}, ${stochKcrossDownD.isSatisfied(endIndex)}, ${stochDUnder50Rule.isSatisfied(endIndex)}")

//    println(s"[${Utils.timestampToLocalDateTime(timestamp)}] ${latestStochCrossesState}")

    currentTrade match {
      case None if longEntryRule.isSatisfied(endIndex) =>
        OpenPosition(strategyName, Position.Long, price, timestamp)
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && longExitRule.isSatisfied(
            endIndex
          ) =>
        ClosePosition(strategyName, Position.Long, price, timestamp)
      case _ =>
        KeepCurrentSate
    }
  }

  private def endIndex = series.getEndIndex

}
