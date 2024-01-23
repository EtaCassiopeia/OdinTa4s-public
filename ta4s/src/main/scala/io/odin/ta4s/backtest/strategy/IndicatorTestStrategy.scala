package io.odin.ta4s.backtest.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.indicators.{
  OdinADXIndicator,
  OdinMACDIndicator,
  SMAIndicator,
  SlopeIndicator,
  StochasticOscillatorDIndicator,
  StochasticOscillatorKIndicator
}
import org.ta4j.core.indicators.adx.{ADXIndicator, MinusDIIndicator, PlusDIIndicator}
import org.ta4j.core.indicators.helpers.{DifferenceIndicator, PreviousValueIndicator, VolumeIndicator}
import org.ta4j.core.indicators.volume.{MVWAPIndicator, VWAPIndicator}
import org.ta4j.core.trading.rules.{OverIndicatorRule, UnderIndicatorRule}

import java.time.ZoneOffset

class IndicatorTestStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T]) extends BackTestStrategy {
  val closePrice = closePriceIndicator.indicator
  val series = closePrice.getBarSeries

  val len = 14
  val barCount = 50

  val plusDIIndicator = new PlusDIIndicator(series, len)
  val minusDIIndicator = new MinusDIIndicator(series, len)
  val adxIndicator = new ADXIndicator(series, len)
  val adxIndicator2 = new OdinADXIndicator(series, len)

  val slopeIndicator = new SlopeIndicator(adxIndicator)
  val slopeIndicator2 = new SlopeIndicator(adxIndicator2)

  val adxSlopePrev = new PreviousValueIndicator(slopeIndicator, 1)
  val adxSlopeDiff = new DifferenceIndicator(slopeIndicator, adxSlopePrev)

  val adxSlopeDiffUnderMinusOne = new UnderIndicatorRule(adxSlopeDiff, -1)
  val adxSlopeDiffUnderOne = new UnderIndicatorRule(adxSlopeDiff, 1)
  val adxSlopeDiffOverOne = new OverIndicatorRule(adxSlopeDiff, 1)

  val macdIndicator = new OdinMACDIndicator(closePrice)
  val stochasticOscillK = new StochasticOscillatorKIndicator(series, 14)
  val k = new SMAIndicator(stochasticOscillK, 3)
  val stochasticOscillD = new StochasticOscillatorDIndicator(stochasticOscillK)
  val d = new StochasticOscillatorDIndicator(k)

  val smaIndicator = new SMAIndicator(closePrice, barCount)
  val volumeIndicator = new VolumeIndicator(series, barCount)

  val vwapIndicator = new VWAPIndicator(series, barCount)
  val mvwapIndicator = new MVWAPIndicator(vwapIndicator, barCount)

  val printStartTimestamp = Utils
    .toLocalDateTime("2020-11-25T00:00")
    .get
    .toInstant(ZoneOffset.UTC)
    .toEpochMilli

  val printEndTimestamp = Utils
    .toLocalDateTime("2020-12-05T23:59")
    .get
    .toInstant(ZoneOffset.UTC)
    .toEpochMilli

  var eventCount = 0

  def evaluate(status: BackTestStatus, price: BigDecimal, timestamp: Long): BackTestStatus = {
    eventCount += 1

    if (timestamp >= printStartTimestamp && timestamp <= printEndTimestamp) {
      val result =
        s"""
         |${Utils.timestampToLocalDateTime(timestamp)}
         |Last stored event time: ${closePrice.getBarSeries.getLastBar.getBeginTime.toString}
         |events: $eventCount
         |index: ${closePrice.getBarSeries.getEndIndex}
         |close price: ${closePrice.getValue(closePrice.getBarSeries.getEndIndex)}
         |price: $price
         |plusDIIndicator: ${plusDIIndicator.getValue(plusDIIndicator.getBarSeries.getEndIndex)}
         |minusDIIndicator: ${minusDIIndicator.getValue(minusDIIndicator.getBarSeries.getEndIndex)}
         |adxIndicator: ${adxIndicator.getValue(adxIndicator.getBarSeries.getEndIndex)}
         |adxIndicator2: ${adxIndicator2.getValue(adxIndicator2.getBarSeries.getEndIndex)}
         |macdIndicator: ${macdIndicator.getValue(macdIndicator.getBarSeries.getEndIndex)}
         |macdLineIndicator: ${macdIndicator.getMACDLineIndicator.getValue(
          macdIndicator.getMACDLineIndicator.getBarSeries.getEndIndex
        )}
         |macdLineSignal: ${macdIndicator.getSignalIndicator.getValue(
          macdIndicator.getSignalIndicator.getBarSeries.getEndIndex
        )}
         |stochasticOscillK: ${stochasticOscillK.getValue(stochasticOscillK.getBarSeries.getEndIndex)}
         |stochasticOscillD: ${stochasticOscillD.getValue(stochasticOscillD.getBarSeries.getEndIndex)}
         |k: ${k.getValue(k.getBarSeries.getEndIndex)}
         |d: ${d.getValue(d.getBarSeries.getEndIndex)}
         |smaIndicator: ${smaIndicator.getValue(smaIndicator.getBarSeries.getEndIndex)}
         |volumeIndicator: ${volumeIndicator.getValue(volumeIndicator.getBarSeries.getEndIndex)}
         |vwapIndicator: ${vwapIndicator.getValue(vwapIndicator.getBarSeries.getEndIndex)}
         |mvwapIndicator: ${mvwapIndicator.getValue(mvwapIndicator.getBarSeries.getEndIndex)}
         |slopeIndicator: ${slopeIndicator.getValue(slopeIndicator.getBarSeries.getEndIndex)}
         |slopeIndicator2: ${slopeIndicator2.getValue(slopeIndicator2.getBarSeries.getEndIndex)}
         |adxSlopeDiff: ${adxSlopeDiff.getValue(adxSlopeDiff.getBarSeries.getEndIndex)}
         |adxSlopeDiffUnderOne: ${adxSlopeDiffUnderOne.isSatisfied(adxSlopeDiff.getBarSeries.getEndIndex)}
         |adxSlopeDiffUnderMinusOne: ${adxSlopeDiffUnderMinusOne.isSatisfied(adxSlopeDiff.getBarSeries.getEndIndex)}
         |adxSlopeDiffOverOne: ${adxSlopeDiffOverOne.isSatisfied(adxSlopeDiff.getBarSeries.getEndIndex)}
         |""".stripMargin

      println(result)
    }
    status
  }

}
