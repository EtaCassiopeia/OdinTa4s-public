package io.odin.ta4s.backtest

import io.odin.common.Utils
import io.odin.ta4s.domain.{Position, Trade, TradingParameters}

import scala.util.Try

case class BackTestStatus(
  initialCapital: BigDecimal = BigDecimal(10000),
  capital: BigDecimal = BigDecimal(10000),
  overallProfit: BigDecimal = BigDecimal(0),
  currentTrade: Option[Trade] = None,
  trades: List[Trade] = List.empty
) extends TradingParameters {
  override def toString: String =
    s"""
      |Default Leverage: $defaultLeverage
      |StopLoss: ${defaultStopLoss.round(Utils.mathContextPrecisionTwo)}
      |Capital: ${capital.round(Utils.mathContextPrecisionFive).doubleValue}
      |Overall profit: ${overallProfit.round(Utils.mathContextPrecisionFive).doubleValue}
      |Profit rate : ${Try((overallProfit * 100 / initialCapital).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
      .getOrElse(0)}
      |Trade count: ${trades.size}
      |Success rate: ${Try(
      trades
        .map(t => t.profit)
        .count(_ > 0) * 100 / trades.size
    ).getOrElse(0)}
      |Trades:
      |
      |Open position: ${currentTrade
      .map(t =>
        s"Buy >> ${Utils.timestampToLocalDateTime(t.entryTimestamp)} @  ${t.entryPrice} [${t.position}] ${t.buyDescription
           .mkString(" ")}"
      )
      .getOrElse("-")}
      |
      |${trades.reverse.mkString("\n")}
      |""".stripMargin
}
