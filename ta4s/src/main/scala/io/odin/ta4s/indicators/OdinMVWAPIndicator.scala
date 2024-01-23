package io.odin.ta4s.indicators

import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.volume.MVWAPIndicator

import scala.concurrent.ExecutionContext

class OdinMVWAPIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  barCount: Int,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinIndicator[T] {
  private val vwap = new OdinVWAPIndicator[T](symbol, name, interval, barCount, autoInitialize)
  val indicator = new MVWAPIndicator(vwap.indicator, barCount)

  def addElement(element: T): Unit = vwap + element

  def +(element: T): Unit = addElement(element)

  def valueAt(index: Int): Either[IndicatorError, Double] = {
    if (vwap.initialized) {
      if (index == -1)
        Left(InsufficientDateError)
      else
        Right(indicator.getValue(index).doubleValue())
    } else
      Left(NotInitializedError)
  }

  def value: Either[IndicatorError, Double] =
    valueAt(lastIndex)

  def lastIndex: Int = vwap.lastIndex

}
