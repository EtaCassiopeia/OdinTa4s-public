package io.odin.ta4s.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.strategy.CrossedState
import io.odin.ta4s.domain.{Position, Trade}
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.indicators.{AverageIndicator, MinusIndicator, OdinADXIndicator, SMAIndicator}
import org.ta4j.core.indicators.adx.ADXIndicator
import org.ta4j.core.indicators.helpers.BooleanTransformIndicator.BooleanTransformType
import org.ta4j.core.indicators.helpers.{
  HighPriceIndicator,
  HighestValueIndicator,
  LowPriceIndicator,
  LowestValueIndicator,
  PreviousValueIndicator
}
import org.ta4j.core.indicators.statistics.SimpleLinearRegressionIndicator
import org.ta4j.core.indicators.volume.ChaikinOscillatorIndicator
import org.ta4j.core.num.Num
import org.ta4j.core.trading.rules.{
  OdinBooleanTransformRule,
  OdinCrossedDownIndicatorRule,
  OdinCrossedUpIndicatorRule,
  OverIndicatorRule,
  UnderIndicatorRule
}

//https://school.stockcharts.com/doku.php?id=technical_indicators:chaikin_oscillator
class OdinChaikinSqueezingMomentumStrategy[T: BarBuilder](
  closePriceIndicator: OdinClosePriceIndicator[T],
  barCount: Int = 50
) extends Strategy {

  private val series = closePriceIndicator.indicator.getBarSeries
  private val closePrice = closePriceIndicator.indicator

  private val adxBarCount = 14
  private val adxIndicator = new OdinADXIndicator(series, adxBarCount)
  private val adxOver20Rule = new OverIndicatorRule(adxIndicator, 20)

  private val chaikinShortBarCount = 3
  private val chaikinLongBarCount = 10
  private val chaikinOscillatorIndicator =
    new ChaikinOscillatorIndicator(series, chaikinShortBarCount, chaikinLongBarCount)

//  private val chaikinCrossedUpZeroRule = new OdinCrossedUpIndicatorRule(chaikinOscillatorIndicator, 0)
  private val chaikinOverZeroRule = new OverIndicatorRule(chaikinOscillatorIndicator, 0)

  private val simpleMovingAverage = new SMAIndicator(closePrice, barCount)
  private val priceOverMovingAverageRule = new OverIndicatorRule(closePrice, simpleMovingAverage)
  private val priceUnderMovingAverageRule = new UnderIndicatorRule(closePrice, simpleMovingAverage)

  //KC Length
  private val lengthKC = 20

  private val ma = new SMAIndicator(closePriceIndicator.indicator, lengthKC)

  private val highestHigh = new HighestValueIndicator(new HighPriceIndicator(series), lengthKC)
  private val lowestLow = new LowestValueIndicator(new LowPriceIndicator(series), lengthKC)

  private val avg1 = new AverageIndicator(highestHigh, lowestLow)
  private val avg = new AverageIndicator(avg1, ma)

  private val squeezingValue = new SimpleLinearRegressionIndicator(new MinusIndicator(closePrice, avg), lengthKC)
  private val previousSqueezingValue = new PreviousValueIndicator(squeezingValue)

  private val numFunction: Double => Num = number => closePrice.numOf(number)

  private val longEntryRule =
    chaikinOverZeroRule.and(priceOverMovingAverageRule)
  //chaikinOverZeroRule.and(adxOver20Rule)

  private val longExitRule =
    priceUnderMovingAverageRule
//    new OdinBooleanTransformRule(squeezingValue, numFunction(0), BooleanTransformType.isGreaterThan)
//    .and(new OdinBooleanTransformRule(squeezingValue, previousSqueezingValue, BooleanTransformType.isLessThan))

  override def evaluate(currentTrade: Option[Trade], timestamp: Long, price: BigDecimal): StrategyEvaluationResult = {
    println(
      Utils.timestampToLocalDateTime(timestamp).toString + " " + chaikinOscillatorIndicator.getValue(
        endIndex
      ) + " " + series.getLastBar.getVolume
    )
    currentTrade match {
      //Enter long position
      case None if series.getBarCount == barCount && longEntryRule.isSatisfied(endIndex) =>
//        println(
//          s"""Open[${Utils.timestampToLocalDateTime(timestamp)}]
//             |Chaikin: ${ chaikinOscillatorIndicator.getValue(endIndex)}
//             |ADX: ${adxIndicator.getValue(endIndex)}
//             |""".stripMargin)
        OpenPosition(strategyName, Position.Long, price, timestamp)

      //Close long position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && longExitRule.isSatisfied(
            endIndex
          ) =>
//        println(
//          s"""Close[${Utils.timestampToLocalDateTime(timestamp)}]
//             |Chaikin: ${ chaikinOscillatorIndicator.getValue(endIndex)}
//             |ADX: ${adxIndicator.getValue(endIndex)}
//             |squeezingValue ${squeezingValue.getValue(endIndex)}
//             |""".stripMargin)
        ClosePosition(strategyName, Position.Long, price, timestamp)

      case _ =>
        KeepCurrentSate
    }
  }

  private def endIndex = series.getEndIndex

}
