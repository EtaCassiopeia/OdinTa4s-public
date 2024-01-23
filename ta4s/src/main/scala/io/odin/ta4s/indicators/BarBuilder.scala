package io.odin.ta4s.indicators

import io.odin.binance.client.model.Candlestick

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}
import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.{Bar, BaseBar}

import scala.language.implicitConversions

trait BarBuilder[T] {
  def toBar(value: T, interval: Interval): Bar
}

object BarBuilder {
  def apply[T](implicit builder: BarBuilder[T]): BarBuilder[T] = builder

  implicit def toZonedDateTime(timestamp: Long): ZonedDateTime =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.of(ZoneOffset.UTC.getId))

  implicit def toJavaBigDecimal(value: BigDecimal): java.math.BigDecimal = value.bigDecimal

  implicit val candleStickBarBuilder: BarBuilder[Candlestick] = (candlestick: Candlestick, interval: Interval) => {
    import candlestick._
    new BaseBar(interval.duration, closeTime, open, high, low, close, volume)
  }

}
