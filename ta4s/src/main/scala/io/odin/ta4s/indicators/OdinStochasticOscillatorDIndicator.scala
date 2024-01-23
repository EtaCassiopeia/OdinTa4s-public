package io.odin.ta4s.indicators

import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.{
  CrossDownIndicator,
  CrossUpIndicator,
  SMAIndicator,
  StochasticOscillatorDIndicator,
  StochasticOscillatorKIndicator
}
import org.ta4j.core.indicators.helpers.{ClosePriceIndicator, HighPriceIndicator, LowPriceIndicator, OpenPriceIndicator}

import scala.concurrent.ExecutionContext

/**
  * There are two components to the stochastic oscillator: the %K and the %D.
  * The %K is the main line indicating the number of time periods, and the %D is the moving average of the %K.
  *
  * https://www.investopedia.com/articles/trading/08/macd-stochastic-double-cross.asp
  */
class OdinStochasticOscillatorDIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  barCount: Int,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinAbstractIndicator[T](symbol, name, interval, barCount, autoInitialize) {

  val stochasticOscillatorKIndicator = new StochasticOscillatorKIndicator(
    new ClosePriceIndicator(series),
//    new OpenPriceIndicator(series),
    barCount,
    new HighPriceIndicator(series),
    new LowPriceIndicator(series)
  )

  val stochasticOscillatorK = new SMAIndicator(stochasticOscillatorKIndicator, barCount)

  val indicator = new StochasticOscillatorDIndicator(stochasticOscillatorKIndicator)

  val crossDownIndicator = new CrossDownIndicator(stochasticOscillatorKIndicator, indicator)
  val crossUpIndicator = new CrossUpIndicator(stochasticOscillatorKIndicator, indicator)

  //Common triggers occur when the %K line drops below 20—the stock is considered oversold, and it is a buying signal.
  def oversold: Boolean =
    stochasticOscillatorKIndicator
      .getValue(stochasticOscillatorKIndicator.getBarSeries.getEndIndex)
      .isLessThan(stochasticOscillatorKIndicator.getBarSeries.numOf(20))

  //If the %K peaks just below 100 and heads downward, the stock should be sold before that value drops below 80.

  //Generally, if the %K value rises above the %D, then a buy signal is indicated by this crossover, provided the values are under 80.
  //If they are above this value, the security is considered overbought.
  def overbought: Boolean =
    stochasticOscillatorKIndicator
      .getValue(stochasticOscillatorKIndicator.getBarSeries.getEndIndex)
      .isGreaterThan(stochasticOscillatorKIndicator.getBarSeries.numOf(80))

  def crossedUp: Boolean = crossUpIndicator.getValue(crossUpIndicator.getBarSeries.getEndIndex)

  def crossedDown: Boolean = crossDownIndicator.getValue(crossDownIndicator.getBarSeries.getEndIndex)

  def details: String = {
    s"""K : ${stochasticOscillatorK.getValue(stochasticOscillatorK.getBarSeries.getEndIndex)}
       |D : ${indicator.getValue(indicator.getBarSeries.getEndIndex)}
       |""".stripMargin
  }

}

/**
  * Equilibrium is the state in which market supply and demand balance each other, and as a result prices become stable
  *
  * Integrating Bullish Crossovers
  *
  * bullish refers to a strong signal for continuously rising prices
  *
  * In the case of a bullish MACD, this will occur when the histogram value is above the equilibrium line, and also when the MACD line is of greater value than the nine-day EMA, also called the "MACD signal line."
  * The stochastic's bullish divergence occurs when %K value passes the %D, confirming a likely price turnaround.
  *
  * Strategy:
  *
  * First, look for the bullish crossovers to occur within two days of each other.
  * When applying the stochastic and MACD double-cross strategy, ideally, the crossover occurs below the 50-line on the stochastic to catch a longer price move.
  * And preferably, you want the histogram value to already be or move higher than zero within two days of placing your trade.
  *
  * MACD must cross slightly after the stochastic, as the alternative could create a false indication of the price trend or place you in a sideways trend.
  */
