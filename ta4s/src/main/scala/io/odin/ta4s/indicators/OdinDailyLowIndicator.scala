package io.odin.ta4s.indicators

import java.time.LocalDateTime

import cats.implicits._
import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.num.Num

class OdinDailyLowIndicator[T: BarBuilder](interval: Interval) extends OdinIndicator[T] {

  var previousLow: Option[Num] = None
  var low: Option[Num] = None

  var lastTimestamp: Option[LocalDateTime] = None

  override def addElement(element: T): Unit = {
    val bar = BarBuilder[T].toBar(element, interval)
    val barTime = bar.getBeginTime.toLocalDateTime
    previousLow = low

    low = (low, lastTimestamp) match {
      case (None, _) =>
        Some(bar.getLowPrice)
      case (Some(_), Some(ts)) if barTime.getDayOfYear > ts.getDayOfYear =>
        Some(bar.getLowPrice)
      case (Some(pLow), _) => Some(pLow.min(bar.getLowPrice))
    }

    lastTimestamp = Some(barTime)
  }

  override def +(element: T): Unit = addElement(element)

  override def valueAt(index: Int): Either[IndicatorError, Double] = {
    index match {
      case i: Int if i == 1 =>
        low.map(_.doubleValue().asRight[IndicatorError]).getOrElse(NotInitializedError.asLeft[Double])
      case i: Int if i == 0 =>
        previousLow.map(_.doubleValue().asRight[IndicatorError]).getOrElse(NotInitializedError.asLeft[Double])
      case _ => OutOfIndexError.asLeft[Double]
    }
  }

  override def value: Either[IndicatorError, Double] =
    low.map(_.doubleValue().asRight[IndicatorError]).getOrElse(NotInitializedError.asLeft[Double])

  override def lastIndex: Int = 1

  def change: Double =
    (for {
      cLow <- low
      pLow <- previousLow
    } yield cLow.minus(pLow).doubleValue()).getOrElse(0)

  def getLastBarTime: LocalDateTime = lastTimestamp.orNull

}
