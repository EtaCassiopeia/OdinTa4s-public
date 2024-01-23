package io.odin.ta4s.strategy.harmonic.patterns

import io.odin.common.Utils
import io.odin.ta4s.domain.{Position, Trade}
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import io.odin.ta4s.strategy.harmonic.patterns.ZigZagPatterns._
import io.odin.ta4s.strategy._
import org.ta4j.core.indicators.OdinBooleanTransformIndicator
import org.ta4j.core.indicators.OdinBooleanTransformIndicator.BooleanTransformType
import org.ta4j.core.indicators.helpers.{HighPriceIndicator, LowPriceIndicator, OpenPriceIndicator}
import org.ta4j.core.num.{NaN, Num}
import org.ta4j.core.{BaseSeries, Indicator, Series}
import io.odin.ta4s.IndicatorUtils._

import scala.util.Try

/**
  * Always test this strategy with one hour interval
  */
class OdinZigZagStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
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

    /*println(
      s"${Utils.timestampToLocalDateTime(timestamp)}: z:${getSeriesValue(zigZagIndicator)} | h:${getIndicatorValue(
        highPriceIndicator
      )} - l:${getIndicatorValue(lowPriceIndicator)} - c:${getIndicatorValue(closePrice)} -  o:${closePrice.getBarSeries.getLastBar.getOpenPrice}"
    )*/

    //I'm not going to use AltTF because it's fucking hard to implement
    //As a workaround we can get candlestick with an hour interval (tf=60)
    //sz = useAltTF ? (change(time(tf)) != 0 ? security(_ticker, tf, zigzag()) : na) : zigzag()
    val sz = zigZagIndicator

    val x = getSeriesValue(sz, 4).getOrElse(NaN.NaN)
    val a = getSeriesValue(sz, 3).getOrElse(NaN.NaN)
    val b = getSeriesValue(sz, 2).getOrElse(NaN.NaN)
    val c = getSeriesValue(sz, 1).getOrElse(NaN.NaN)
    val d = getSeriesValue(sz, 0).getOrElse(NaN.NaN)

    val xab = abs(b - a) / abs(x - a)
    val xad = abs(a - d) / abs(x - a)
    val abc = abs(b - c) / abs(a - b)
    val bcd = abs(c - d) / abs(b - c)

    val fib_range = abs(d - c)
    import zigZagPatterns._

    val buy_patterns_00 = isABCD(1d, xab, xad, abc, bcd, d, c)
      .or(isBat(1d, xab, xad, abc, bcd, d, c))
      .or(isAltBat(1d, xab, xad, abc, bcd, d, c))
      .or(isButterfly(1d, xab, xad, abc, bcd, d, c))
      .or(isGartley(1d, xab, xad, abc, bcd, d, c))
      .or(isCrab(1d, xab, xad, abc, bcd, d, c))
      .or(isShark(1d, xab, xad, abc, bcd, d, c))
      .or(is5o(1d, xab, xad, abc, bcd, d, c))
      .or(isWolf(1d, xab, xad, abc, bcd, d, c))
      .or(isHnS(1d, xab, xad, abc, bcd, d, c))
      .or(isConTria(1d, xab, xad, abc, bcd, d, c))
      .or(isExpTria(1d, xab, xad, abc, bcd, d, c))

    val buy_patterns_01 = isAntiBat(1d, xab, xad, abc, bcd, d, c)
      .or(isAntiButterfly(1d, xab, xad, abc, bcd, d, c))
      .or(isAntiGartley(1d, xab, xad, abc, bcd, d, c))
      .or(isAntiCrab(1d, xab, xad, abc, bcd, d, c))
      .or(isAntiShark(1d, xab, xad, abc, bcd, d, c))

    val sel_patterns_00 = isABCD(-1d, xab, xad, abc, bcd, d, c)
      .or(isBat(-1d, xab, xad, abc, bcd, d, c))
      .or(isAltBat(-1d, xab, xad, abc, bcd, d, c))
      .or(isButterfly(-1d, xab, xad, abc, bcd, d, c))
      .or(isGartley(-1d, xab, xad, abc, bcd, d, c))
      .or(isCrab(-1d, xab, xad, abc, bcd, d, c))
      .or(isShark(-1d, xab, xad, abc, bcd, d, c))
      .or(is5o(-1d, xab, xad, abc, bcd, d, c))
      .or(isWolf(-1d, xab, xad, abc, bcd, d, c))
      .or(isHnS(-1d, xab, xad, abc, bcd, d, c))
      .or(isConTria(-1d, xab, xad, abc, bcd, d, c))
      .or(isExpTria(-1d, xab, xad, abc, bcd, d, c))

    val sel_patterns_01 = isAntiBat(-1d, xab, xad, abc, bcd, d, c)
      .or(isAntiButterfly(-1d, xab, xad, abc, bcd, d, c))
      .or(isAntiGartley(-1d, xab, xad, abc, bcd, d, c))
      .or(isAntiCrab(-1d, xab, xad, abc, bcd, d, c))
      .or(isAntiShark(-1d, xab, xad, abc, bcd, d, c))

    val buy_entry = (buy_patterns_00
      .or(buy_patterns_01))
      .and(getIndicatorValue(closePrice).get <= f_last_fib(ew_rate, fib_range, d, c))
    val buy_close = (getIndicatorValue(highPriceIndicator).get >= f_last_fib(tp_rate, fib_range, d, c))
      .or(getIndicatorValue(lowPriceIndicator).get <= f_last_fib(sl_rate, fib_range, d, c))
    val sel_entry = (sel_patterns_00
      .or(sel_patterns_01))
      .and(getIndicatorValue(closePrice).get >= f_last_fib(ew_rate, fib_range, d, c))
    val sel_close = (getIndicatorValue(lowPriceIndicator).get <= f_last_fib(tp_rate, fib_range, d, c))
      .or(getIndicatorValue(highPriceIndicator).get >= f_last_fib(sl_rate, fib_range, d, c))

    currentTrade match {

      //Close Long position
      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Long && buy_close =>
        ClosePosition(strategyName, Position.Long, price, timestamp)

      //Close Short position
      case Some(trade) if timestamp > trade.entryTimestamp && trade.position == Position.Short && sel_close =>
        ClosePosition(strategyName, Position.Short, price, timestamp)

      //Open Long position
      case None if series.getBarCount >= barCount && buy_entry =>
        OpenPosition(strategyName, Position.Long, price, timestamp)

      //Open Short position
      case None if series.getBarCount >= barCount && sel_entry =>
        OpenPosition(strategyName, Position.Short, price, timestamp)

      case _ => KeepCurrentSate
    }

  }
}
