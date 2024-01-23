package io.odin.ta4s.domain

import io.odin.common.Utils

import scala.util.Try

case class Trade(
  position: Position,
  amount: BigDecimal,
  entryTimestamp: Long,
  entryPrice: BigDecimal,
  exitTimestamp: Long = 0,
  exitPrice: BigDecimal = BigDecimal(0),
  soldOnStopLoss: Boolean = false,
  buyDescription: List[String] = List.empty,
  sellDescription: List[String] = List.empty
) extends TradingParameters {

  //Profit = S.P – C.P.
  def profit = {
    val p =
      if (position == Position.Long)
        amount * (exitPrice - entryPrice)
      else
        amount * (entryPrice - exitPrice)

    (p * defaultLeverage).round(Utils.mathContextPrecisionTwo)
  }

  //Profit * 100 / C.P.
  def profitPercentage: BigDecimal = Try(profit * 100 / (entryPrice * amount)).getOrElse(0)

  override def toString: String =
    s"""Position >> $position
       |Amount   >> ${amount.round(Utils.mathContextPrecisionTwo)}
       |Buy      >> ${Utils.timestampToLocalDateTime(entryTimestamp)} @  $entryPrice ${buyDescription.mkString(" ")}
       |Sell     >> ${Utils.timestampToLocalDateTime(exitTimestamp)} @ $exitPrice ${if (soldOnStopLoss) "*"
    else ""} ${sellDescription.mkString(" ")}
       |Profit   >> ${profit.doubleValue} [${profitPercentage.round(Utils.mathContextPrecisionTwo).doubleValue}%]
       |""".stripMargin
}
