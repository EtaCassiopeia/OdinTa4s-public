package io.odin.ta4s.backtest.strategy

import io.odin.common.Utils
import io.odin.ta4s.backtest.BackTestStatus
import io.odin.ta4s.domain.Position
import io.odin.ta4s.indicators.{BarBuilder, OdinClosePriceIndicator}
import org.ta4j.core.Rule
import org.ta4j.core.indicators.SMAIndicator
import org.ta4j.core.indicators.adx.{ADXIndicator, MinusDIIndicator, PlusDIIndicator}
import org.ta4j.core.trading.rules.{
  CrossedDownIndicatorRule,
  CrossedUpIndicatorRule,
  OverIndicatorRule,
  UnderIndicatorRule
}

class ADXStrategy[T: BarBuilder](closePriceIndicator: OdinClosePriceIndicator[T], barCount: Int = 50)
    extends BackTestStrategy {

  val series = closePriceIndicator.indicator.getBarSeries
  val smaIndicator = new SMAIndicator(closePriceIndicator.indicator, barCount)

  val adxBarCount = 14
  val adxIndicator = new ADXIndicator(series, adxBarCount)
  val adxOver20Rule = new OverIndicatorRule(adxIndicator, 20)

  val plusDIIndicator = new PlusDIIndicator(series, adxBarCount)
  val minusDIIndicator = new MinusDIIndicator(series, adxBarCount)

  val plusDICrossedUpMinusDI = new CrossedUpIndicatorRule(plusDIIndicator, minusDIIndicator)
  val plusDICrossedDownMinusDI = new CrossedDownIndicatorRule(plusDIIndicator, minusDIIndicator)
  val closePriceOverSma = new OverIndicatorRule(closePriceIndicator.indicator, smaIndicator)
  val entryRule: Rule = adxOver20Rule.and(plusDICrossedUpMinusDI).and(closePriceOverSma)

  val closePriceUnderSma = new UnderIndicatorRule(closePriceIndicator.indicator, smaIndicator)
  val exitRule: Rule = adxOver20Rule.and(plusDICrossedDownMinusDI).and(closePriceUnderSma)

  def evaluate(backTestStatus: BackTestStatus, currentPrice: BigDecimal, timestamp: Long): BackTestStatus = {
    backTestStatus.currentTrade match {
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && currentPrice < trade.entryPrice * (1 - defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)

      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Short && currentPrice > trade.entryPrice * (1 + defaultStopLoss) =>
        println(s"Panic Sell, [${Utils.timestampToLocalDateTime(timestamp)}] at $currentPrice ")
        sell(backTestStatus, currentPrice, timestamp, soldOnStopLoss = true)

      //Close Long position
      case Some(trade)
          if timestamp > trade.entryTimestamp && trade.position == Position.Long && exitRule.isSatisfied(
            getLastIndex
          ) =>
        sell(backTestStatus, currentPrice, timestamp)

      //Open Long position
      case None if series.getBarCount >= barCount && entryRule.isSatisfied(getLastIndex) =>
        buy(backTestStatus, currentPrice, timestamp)

      case _ => backTestStatus
    }
  }

  private def getLastIndex = series.getEndIndex
}
