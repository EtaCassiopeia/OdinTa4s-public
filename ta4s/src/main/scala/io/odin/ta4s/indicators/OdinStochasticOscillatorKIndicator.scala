package io.odin.ta4s.indicators

import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.StochasticOscillatorKIndicator

import scala.concurrent.ExecutionContext

class OdinStochasticOscillatorKIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  barCount: Int = 14,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinAbstractIndicator[T](symbol, name, interval, barCount, autoInitialize) {
  val indicator = new StochasticOscillatorKIndicator(series, barCount)
}
