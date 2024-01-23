package io.odin.ta4s.indicators

import java.time.LocalDateTime

import cats.implicits._
import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.num.Num

class OdinDailyHighIndicator[T: BarBuilder](interval: Interval) extends OdinIndicator[T] {

  var previousHigh: Option[Num] = None
  var high: Option[Num] = None

  var lastTimestamp: Option[LocalDateTime] = None

  override def addElement(element: T): Unit = {

    val bar = BarBuilder[T].toBar(element, interval)
    val barTime = bar.getBeginTime.toLocalDateTime
    previousHigh = high

    high = (high, lastTimestamp) match {
      case (None, _) =>
        Some(bar.getHighPrice)
      case (Some(_), Some(ts)) if barTime.getDayOfYear > ts.getDayOfYear =>
        Some(bar.getHighPrice)
      case (Some(pHigh), _) => Some(pHigh.max(bar.getHighPrice))
    }

    lastTimestamp = Some(barTime)
  }

  override def +(element: T): Unit = addElement(element)

  override def valueAt(index: Int): Either[IndicatorError, Double] = {
    index match {
      case i: Int if i == 1 =>
        high.map(_.doubleValue().asRight[IndicatorError]).getOrElse(NotInitializedError.asLeft[Double])
      case i: Int if i == 0 =>
        previousHigh.map(_.doubleValue().asRight[IndicatorError]).getOrElse(NotInitializedError.asLeft[Double])
      case _ => OutOfIndexError.asLeft[Double]
    }
  }

  override def value: Either[IndicatorError, Double] =
    high.map(_.doubleValue().asRight[IndicatorError]).getOrElse(NotInitializedError.asLeft[Double])

  override def lastIndex: Int = 1

  def change: Double =
    (for {
      cHigh <- high
      pHigh <- previousHigh
    } yield cHigh.minus(pHigh).doubleValue()).getOrElse(0)

  def getLastBarTime: LocalDateTime = lastTimestamp.orNull
}
