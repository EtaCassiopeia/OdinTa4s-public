package io.odin.ta4s.indicators

import io.odin.binance.client.HistoricalDataFetcher
import io.odin.binance.client.model.Candlestick

import java.util.concurrent.LinkedBlockingQueue
import io.odin.binance.client.model.CandlestickRequest.Interval
import org.ta4j.core.indicators.CachedIndicator
import org.ta4j.core.num.Num
import org.ta4j.core.{Bar, BarSeries, BaseBarSeriesBuilder}
import zio.{Runtime, ZIO}

import scala.concurrent.{ExecutionContext, Future}

abstract class OdinAbstractIndicator[T: BarBuilder](
  symbol: String,
  name: String,
  interval: Interval,
  barCount: Int,
  autoInitialize: Boolean
)(implicit
  ec: ExecutionContext
) extends OdinIndicator[T] {
  protected val series: BarSeries = new BaseBarSeriesBuilder().withMaxBarCount(barCount).withName(name).build

  val indicator: CachedIndicator[Num]

  private val buffer = new LinkedBlockingQueue[Bar]()
  private var _initialized = false

  def initialized: Boolean = _initialized

  if (autoInitialize)
    Future {
      Runtime.default.unsafeRun(
        HistoricalDataFetcher
          .candleStickStream(symbol, interval, barCount)
          .foreach(candleStick => ZIO.effect(addBar(BarBuilder[Candlestick].toBar(candleStick, interval))))
      )
    }.onComplete { _ =>
      synchronized {
        replayBuffer()
        _initialized = true
      }
    }
  else
    _initialized = true

  private def checkAndAdd(bar: Bar): Unit =
    if (series.isEmpty || bar.getEndTime.compareTo(series.getLastBar.getEndTime) > 0) {
      series.addBar(bar)
    } else if (bar.getEndTime.compareTo(series.getLastBar.getEndTime) == 0) {
      println(s"Replacing bar ${bar.toString}")
      series.addBar(bar, true)
    } else {
      println(s"Outdated bar ${bar.toString}")
    }

  private def replayBuffer(): Unit = {
    while (!buffer.isEmpty) {
      val bar = buffer.take()
      checkAndAdd(bar)
    }
  }

  private[indicators] def addBar(bar: Bar): Unit =
    synchronized {
      if (!initialized)
        buffer.put(bar)
      else
        checkAndAdd(bar)
    }

  def addElement(element: T): Unit = addBar(BarBuilder[T].toBar(element, interval))

  def +(element: T): Unit = addElement(element)

  def valueAt(index: Int): Either[IndicatorError, Double] = {
    if (initialized)
      if (index == -1)
        Left(InsufficientDateError)
      else {
        Right(indicator.getValue(index).doubleValue())
      }
    else
      Left(NotInitializedError)
  }

  def value: Either[IndicatorError, Double] =
    valueAt(lastIndex)

  def lastIndex: Int =
    series.getEndIndex

}
