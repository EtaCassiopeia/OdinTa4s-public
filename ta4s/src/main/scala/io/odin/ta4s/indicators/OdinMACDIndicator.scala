package io.odin.ta4s.indicators

import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.MACDIndicator
import org.ta4j.core.indicators.helpers.{ClosePriceIndicator, OpenPriceIndicator}

import scala.concurrent.ExecutionContext

class OdinMACDIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  shortBarCount: Int = 12,
  longBarCount: Int = 26,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinAbstractIndicator[T](symbol, name, interval, longBarCount, autoInitialize) {

  val priceIndicator = new ClosePriceIndicator(series)
//  val priceIndicator = new OpenPriceIndicator(series)

  val indicator = new MACDIndicator(priceIndicator, shortBarCount, longBarCount)
}
