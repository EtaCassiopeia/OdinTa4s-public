package io.odin.ta4s.strategy

import io.odin.common.Utils
import io.odin.ta4s.domain.{Position, Trade}
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.indicators.helpers.BooleanTransformIndicator.BooleanTransformType
import org.ta4j.core.indicators.helpers._
import org.ta4j.core.indicators.statistics.{SimpleLinearRegressionIndicator, StandardDeviationIndicator}
import org.ta4j.core.indicators.{AverageIndicator, MinusIndicator, OdinADXIndicator, PlusIndicator, SMAIndicator}
import org.ta4j.core.num.Num
import org.ta4j.core.trading.rules.{
  OdinBooleanTransformRule,
  OdinCrossedDownIndicatorRule,
  OdinCrossedUpIndicatorRule,
  OverIndicatorRule
}
import io.odin.ta4s.IndicatorUtils._
import io.odin.ta4s.backtest.strategy.CrossedState
import org.ta4j.core.indicators
import org.ta4j.core.indicators.adx.ADXIndicator
import org.ta4j.core.indicators.volume.ChaikinOscillatorIndicator

class OdinSqueezeMomentumStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends Strategy {

  private val series = closePriceIndicator.indicator.getBarSeries
  private val closePrice = closePriceIndicator.indicator

  private val adxBarCount = 14
  private val adxIndicator = new OdinADXIndicator(series, adxBarCount)
  private val adxOver20Rule = new OverIndicatorRule(adxIndicator, 20)

  private val chaikinShortBarCount = 3
  private val chaikinLongBarCount = 10
  private val chaikinOscillatorIndicator =
    new ChaikinOscillatorIndicator(series, chaikinShortBarCount, chaikinLongBarCount)
  private val chaikin0Rule = new OverIndicatorRule(chaikinOscillatorIndicator, 0)

  private var recentMACDCrossesState: Option[CrossedState] = None

  private val shortBarCount: Int = 12
  private val longBarCount: Int = 26
  private val signalBarCount: Int = 9

  private val macdIndicator =
    new indicators.OdinMACDIndicator(closePrice, shortBarCount, longBarCount, signalBarCount)

  private val macdLineIndicator = macdIndicator.getMACDLineIndicator
  private val macdSignalIndicator = macdIndicator.getSignalIndicator

  private val macdLineCrossedUpSignalRule = new OdinCrossedUpIndicatorRule(macdLineIndicator, macdSignalIndicator)
  private val macdLineCrossedDownSignalRule = new OdinCrossedDownIndicatorRule(macdLineIndicator, macdSignalIndicator)

  //BB Length
  private val length = 20

  //BB MultFactor
  private val mult = 2.0

  //KC Length
  private val lengthKC = 20

  //KC MultFactor
  private val multKC = 1.5

  // Calculate BB
  private val basis = new SMAIndicator(closePriceIndicator.indicator, length)
  private val stdev = new StandardDeviationIndicator(closePriceIndicator.indicator, length)
  private val dev = new MultiplierIndicator(stdev, multKC)
  private val upperBB = new PlusIndicator(basis, dev)
  private val lowerBB = new MinusIndicator(basis, dev)

  // Calculate KC
  private val ma = new SMAIndicator(closePriceIndicator.indicator, lengthKC)
  private val range = new TRIndicator(series)
  private val rangema = new SMAIndicator(range, lengthKC)

  //ma + rangema * multKC
  val upperKC = new PlusIndicator(ma, new MultiplierIndicator(rangema, multKC))

  //ma - rangema * multKC
  val lowerKC = new MinusIndicator(ma, new MultiplierIndicator(rangema, multKC))

  private val sqzOn = new OdinBooleanTransformRule(lowerBB, lowerKC, BooleanTransformType.isGreaterThan)
    .and(new OdinBooleanTransformRule(upperBB, upperKC, BooleanTransformType.isLessThan))
  private val sqzOff = new OdinBooleanTransformRule(lowerBB, lowerKC, BooleanTransformType.isLessThan)
    .and(new OdinBooleanTransformRule(upperBB, upperKC, BooleanTransformType.isGreaterThan))

  private val noSqz = sqzOn.negation().and(sqzOff.negation())

  private val highestHigh = new HighestValueIndicator(new HighPriceIndicator(series), lengthKC)
  private val lowestLow = new LowestValueIndicator(new LowPriceIndicator(series), lengthKC)

  private val avg1 = new AverageIndicator(highestHigh, lowestLow)
  private val avg = new AverageIndicator(avg1, ma)

  private val value = new SimpleLinearRegressionIndicator(new MinusIndicator(closePrice, avg), lengthKC)
  private val previousValue = new PreviousValueIndicator(value)
  private val oneBeforePreviousValue = new PreviousValueIndicator(value, 2)

  val valueOver100Rule = new OverIndicatorRule(value, 100)

  private val numFunction: Double => Num = number => closePrice.numOf(number)

//  private val longEntryRule = new OdinBooleanTransformRule(value, numFunction(0), BooleanTransformType.isLessThan)
//    .and(new OdinBooleanTransformRule(value, previousValue, BooleanTransformType.isGreaterThan))
//  .and(sqzOn)
//
//  private val longExitRule = new OdinBooleanTransformRule(value, numFunction(0), BooleanTransformType.isGreaterThan)
//    .and(new OdinBooleanTransformRule(value, previousValue, BooleanTransformType.isLessThan))
//  .and(sqzOn)

  private val shortEntryRule = new OdinBooleanTransformRule(value, numFunction(0), BooleanTransformType.isGreaterThan)
    .and(new OdinBooleanTransformRule(value, previousValue, BooleanTransformType.isLessThan))
    //.and(new OdinBooleanTransformRule(oneBeforePreviousValue, previousValue, BooleanTransformType.isLessThan))
    .and(adxOver20Rule)
    .and(chaikin0Rule)
  //.and(valueOver100Rule)
  //.and(sqzOff)

  private val shortExitRule = new OdinBooleanTransformRule(value, numFunction(0), BooleanTransformType.isLessThan)
    .and(new OdinBooleanTransformRule(value, previousValue, BooleanTransformType.isGreaterThan))

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult = {

    recentMACDCrossesState =
      (macdLineCrossedUpSignalRule.isSatisfied(endIndex), macdLineCrossedDownSignalRule.isSatisfied(endIndex)) match {
        case (true, false) =>
          //println(s"Crossed up at ${Utils.timestampToLocalDateTime(timestamp)}")
          Some(CrossedState.CrossedUp)
        case (false, true) =>
          //println(s"Crossed down at ${Utils.timestampToLocalDateTime(timestamp)}")
          Some(CrossedState.CrossedDown)
        case _ => recentMACDCrossesState
      }

    /*println(s"[${Utils.timestampToLocalDateTime(timestamp)}], price: $price, value: ${getIndicatorValue(value)
      .getOrElse(numFunction(0))} ${if(noSqz.isSatisfied(endIndex)) "Blue" else if(sqzOn.isSatisfied(endIndex)) "Black" else "Grey"}")*/
    currentTrade match {
      //Enter long position
//      case None if series.getBarCount == barCount && longEntryRule.isSatisfied(endIndex) =>
//        OpenPosition(strategyName, Position.Long, price, timestamp)
//
//      //Close long position
//      case Some(trade)
//          if timestamp > trade.entryTimestamp && trade.position == Position.Long && longExitRule.isSatisfied(
//            endIndex
//          ) =>
//        ClosePosition(strategyName, Position.Long, price, timestamp)

      //Enter short position
      case None
          if series.getBarCount == barCount && recentMACDCrossesState
            .fold(false)(crossed => crossed == CrossedState.CrossedUp) && shortEntryRule.isSatisfied(endIndex) =>
        //clear current crossed state
        recentMACDCrossesState = None
        OpenPosition(strategyName, Position.Short, price, timestamp)

      //Close short position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && recentMACDCrossesState.fold(false)(
            crossed => crossed == CrossedState.CrossedDown
          ) && shortExitRule.isSatisfied(
            endIndex
          ) =>
        //clear current crossed state
        recentMACDCrossesState = None
        ClosePosition(strategyName, Position.Short, price, timestamp)

      case _ =>
        KeepCurrentSate
    }
  }

  private def endIndex = series.getEndIndex

}
