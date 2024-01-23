package io.odin.ta4s.strategy.harmonic.patterns

import io.odin.ta4s.domain.{Position, Trade}
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import io.odin.ta4s.strategy._
import io.odin.ta4s.strategy.harmonic.patterns.ZigZagPatterns._
import org.ta4j.core.indicators.OdinBooleanTransformIndicator
import org.ta4j.core.indicators.OdinBooleanTransformIndicator.BooleanTransformType
import org.ta4j.core.indicators.helpers.{HighPriceIndicator, LowPriceIndicator, OpenPriceIndicator}
import org.ta4j.core.num.{NaN, Num}
import org.ta4j.core.{BaseSeries, Indicator, Series}

import scala.util.Try

/**
  * Always test this strategy with one hour interval
  */
class OdinZigZagSimpleStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends Strategy {

  private val series = closePriceIndicator.indicator.getBarSeries
  private val closePrice = closePriceIndicator.indicator
  private val openPrice = new OpenPriceIndicator(series)

  private val isUpIndicator =
    new OdinBooleanTransformIndicator(closePrice, openPrice, BooleanTransformType.isGreaterThanOrEqual)
  private val isDownIndicator =
    new OdinBooleanTransformIndicator(closePrice, openPrice, BooleanTransformType.isLessThanOrEqual)
  private val directionIndicator = new BaseSeries[Int]()
  private val zigZagIndicator = new BaseSeries[Num]()

  val lowPriceIndicator = new LowPriceIndicator(series)
  val highPriceIndicator = new HighPriceIndicator(series)

  var previousKnownZigZag = NaN.NaN

  private def getIndicatorValue[A](indicator: Indicator[A], backwardSteps: Int = 0) =
    Try {
      val index = indicator.getBarSeries.getEndIndex - backwardSteps
      indicator.getValue(index)
    }

  private def highest(backwardSteps: Int = 0): Try[Num] = {
    if (backwardSteps == 0)
      getIndicatorValue(highPriceIndicator, backwardSteps)
    else {
      Try((0 to backwardSteps).map { i =>
        getIndicatorValue(highPriceIndicator, i).getOrElse(NaN.NaN)
      }.max)
    }
  }

  private def lowest(backwardSteps: Int = 0): Try[Num] = {
    if (backwardSteps == 0)
      getIndicatorValue(lowPriceIndicator, backwardSteps)
    else {
      Try((0 to backwardSteps).map { i =>
        getIndicatorValue(lowPriceIndicator, i).getOrElse(NaN.NaN)
      }.min)
    }
  }

  private def getSeriesValue[A](series: Series[A], backwardSteps: Int = 0): Try[A] =
    Try {
      val index = series.getEndIndex - backwardSteps
      series.getBar(index)
    }

  private def nz(series: Series[Int], backwardSteps: Int = 0): Int = getSeriesValue(series, backwardSteps).getOrElse(0)

  private implicit val numFunction: Double => Num = number => closePrice.numOf(number)

  val zigZagPatterns = new ZigZagPatterns()

  private def f_last_fib(_rate: Num, fib_range: Num, d: Num, c: Num) =
    if (d > c) d - (fib_range * _rate) else d + (fib_range * _rate)

  private val ew_rate = 0.382
  private val tp_rate = 0.618
  private val sl_rate = -0.618

  /**
    * zigzag() =>
    *  _isUp = close >= open
    *  _isDown = close <= open
    *  _direction = _isUp[1] and _isDown ? -1 : _isDown[1] and _isUp ? 1 : nz(_direction[1])
    *  _zigzag = _isUp[1] and _isDown and _direction[1] != -1 ? highest(2) : _isDown[1] and _isUp and _direction[1] != 1 ? lowest(2) : na
    */
  private def zigZag(): Try[Unit] =
    for {
      previousIsUp <- getIndicatorValue(isUpIndicator, 1)
      currentIsUp <- getIndicatorValue(isUpIndicator)
      previousIsDown <- getIndicatorValue(isDownIndicator, 1)
      currentIsDown <- getIndicatorValue(isDownIndicator)
      direction =
        if (previousIsUp && currentIsDown)
          -1
        else if (previousIsDown && currentIsUp)
          1
        else
          nz(directionIndicator, 0)
      _ <- Try(directionIndicator.addBar(direction))

      previousDirection <- getSeriesValue(directionIndicator, 1)
      zigzag <-
        if (previousIsUp && currentIsDown && previousDirection != -1)
          highest(1)
        else if (previousIsDown && currentIsUp && previousDirection != 1)
          lowest(1)
        else Try(NaN.NaN)
      _ <- Try(zigZagIndicator.addBar(zigzag))
    } yield ()

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult = {
    //Calculate
    zigZag()

    val currentZigZagValue = getSeriesValue(zigZagIndicator).getOrElse(NaN.NaN)

    val evaluationResult = currentTrade match {

      //Close Long position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && currentZigZagValue.isGreaterThan(
            previousKnownZigZag
          ) =>
        ClosePosition(strategyName, Position.Long, price, timestamp)

      //Close Short position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && currentZigZagValue.isLessThan(
            previousKnownZigZag
          ) =>
        ClosePosition(strategyName, Position.Short, price, timestamp)

      //Open Long position
      case None
          if series.getBarCount >= barCount && !currentZigZagValue.isNaN && !previousKnownZigZag.isNaN && currentZigZagValue
            .isLessThan(previousKnownZigZag) =>
        OpenPosition(strategyName, Position.Long, price, timestamp)

      //Open Short position
      case None
          if series.getBarCount >= barCount && !currentZigZagValue.isNaN && !previousKnownZigZag.isNaN && currentZigZagValue
            .isGreaterThan(previousKnownZigZag) =>
        OpenPosition(strategyName, Position.Short, price, timestamp)

      case _ => KeepCurrentSate
    }

    if (!currentZigZagValue.isNaN)
      previousKnownZigZag = currentZigZagValue

    evaluationResult

  }
}
