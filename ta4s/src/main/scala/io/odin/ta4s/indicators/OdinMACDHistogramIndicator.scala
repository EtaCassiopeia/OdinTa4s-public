package io.odin.ta4s.indicators

import java.time.LocalDateTime
import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators
import org.ta4j.core.indicators.helpers.{ClosePriceIndicator, DifferenceIndicator, OpenPriceIndicator}
import org.ta4j.core.indicators.{EMAIndicator, OdinMACDIndicator}

import scala.concurrent.ExecutionContext

class OdinMACDHistogramIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  shortBarCount: Int = 12,
  longBarCount: Int = 26,
  signalBarCount: Int = 9,
  autoInitialize: Boolean = true
)(implicit
  ec: ExecutionContext
) extends OdinAbstractIndicator[T](symbol, name, interval, longBarCount, autoInitialize) {

//  private val priceIndicator = new OpenPriceIndicator(series)
  private val priceIndicator = new ClosePriceIndicator(series)

  val indicator =
    new indicators.OdinMACDIndicator(priceIndicator, shortBarCount, longBarCount, signalBarCount)

  def getSignalIndicator: EMAIndicator = indicator.getSignalIndicator

  def getMACDIndicator: DifferenceIndicator = indicator.getMACDLineIndicator

  def getSignal: Double = indicator.getSignal(lastIndex).doubleValue()

  def getMACDLine: Double = indicator.getMACDLine(lastIndex).doubleValue()

  def crossedDown: Boolean = indicator.isMACDLineCrossDownSignal
  def crossedUp: Boolean = indicator.isMACDLineCrossUpSignal

  def getLastBarTime: LocalDateTime = indicator.getLastBarDateTime
}
