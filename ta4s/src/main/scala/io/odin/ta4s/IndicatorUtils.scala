package io.odin.ta4s

import org.ta4j.core.{Indicator, Series}
import org.ta4j.core.num.Num

import scala.util.Try

object IndicatorUtils {

  def getIndicatorValue[A](indicator: Indicator[A], backwardSteps: Int = 0): Try[A] =
    Try {
      val index = indicator.getBarSeries.getEndIndex - backwardSteps
      indicator.getValue(index)
    }

  def getSeriesValue[A](series: Series[A], backwardSteps: Int = 0): Try[A] =
    Try {
      val index = series.getEndIndex - backwardSteps
      series.getBar(index)
    }

  def nz(series: Series[Int], backwardSteps: Int = 0): Int = getSeriesValue(series, backwardSteps).getOrElse(0)

}
