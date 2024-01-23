package io.odin.ta4s.indicators

import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.volume.VWAPIndicator

import scala.concurrent.ExecutionContext

class OdinVWAPIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  barCount: Int,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinAbstractIndicator[T](symbol, name, interval, barCount, autoInitialize) {
  val indicator: VWAPIndicator = new VWAPIndicator(series, barCount)
}
