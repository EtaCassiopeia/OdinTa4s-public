package io.odin.ta4s.indicators

import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.SMAIndicator
import org.ta4j.core.indicators.helpers.{ClosePriceIndicator, OpenPriceIndicator}

import scala.concurrent.ExecutionContext

class OdinSMAIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  barCount: Int,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinAbstractIndicator[T](symbol, name, interval, barCount, autoInitialize) {

  private val priceIndicator = new OpenPriceIndicator(series)

  val indicator = new SMAIndicator(priceIndicator, barCount)
}
