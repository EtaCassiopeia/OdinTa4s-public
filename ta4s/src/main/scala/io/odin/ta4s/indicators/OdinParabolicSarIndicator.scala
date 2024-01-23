package io.odin.ta4s.indicators

import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.ParabolicSarIndicator

import scala.concurrent.ExecutionContext

object MarketTrend extends Enumeration {
  type MarketTrend = Value
  val DownTrend, UpTrend, Unknown = Value
}

class OdinParabolicSarIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  barCount: Int,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinAbstractIndicator[T](symbol, name, interval, barCount, autoInitialize) {
  val indicator = new ParabolicSarIndicator(series)

  def marketTrend: MarketTrend.Value =
    value
      .flatMap { currentValue =>
        valueAt(lastIndex - 1).map(previousValue =>
          if (currentValue < previousValue) MarketTrend.DownTrend
          else if (currentValue > previousValue) MarketTrend.UpTrend
          else MarketTrend.Unknown
        )
      }
      .getOrElse(MarketTrend.Unknown)
}
