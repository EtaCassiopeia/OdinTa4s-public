package io.odin.ta4s.indicators

import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.{EMAIndicator, MACDIndicator}

import scala.concurrent.ExecutionContext

class OdinEMAMACDIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  shortBarCount: Int = 12,
  longBarCount: Int = 26,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinAbstractIndicator[T](symbol, name, interval, longBarCount, autoInitialize) {

  val macd = new MACDIndicator(new ClosePriceIndicator(series), shortBarCount, longBarCount)

  val indicator = new EMAIndicator(macd, 18)
}
