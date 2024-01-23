package io.odin.crypto.ml

import cats.syntax.functor._
import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.circe.Decoder
import io.circe.generic.auto._
import io.odin.crypto.ml.Payload.OrderSide

/**
  * https://crypto-ml.com/api/
  */
sealed trait Payload

//{"bullBear":"BULL 13.56","_7dayTrend":"Bull market weakening."}
case class MarketIndex(bullBear: String, `_7dayTrend`: String) extends Payload

//{"side":"sell", "productCode":"BTC-USD", "price":"9543.01", "time":"2020-02-24T18:38:01.6484377Z", "bot":"ML"}
//{"side":"buy", "productCode":"ETH-USD", "price":"264.56", "time":"2020-02-22T01:22:42.2901814Z", "trailingStop":"-0.0397119140625", "buyTrigger":"3.07325747536978", "tslAdjust":"-0.00591409301757813", "nnValue":"5.20176589036058", "bot":"ML"}
case class TradeAlert(
  side: OrderSide,
  productCode: String,
  price: BigDecimal,
  time: String,
  bot: String,
  trailingStop: Option[BigDecimal] = None,
  buyTrigger: Option[BigDecimal] = None,
  tslAdjust: Option[BigDecimal] = None,
  nnValue: Option[BigDecimal] = None
) extends Payload

//{"crypto":"USD-ETH", "prediction_6hr":"0.10868181010881219", "prediction_12hr":"-2.6982040678439647", "manip_value":"1"}
case class PricePrediction(crypto: String, prediction_6hr: BigDecimal, prediction_12hr: BigDecimal, manip_value: Int)
    extends Payload

sealed trait WebSocketError extends Payload
case class ErrorResponse(msg: String) extends WebSocketError

object Payload {

  sealed trait OrderSide extends EnumEntry

  object OrderSide extends Enum[OrderSide] with CirceEnum[OrderSide] {
    val values = findValues

    case object sell extends OrderSide

    case object buy extends OrderSide

    case object unknown extends OrderSide
  }

  implicit val decodeEvent: Decoder[Payload] =
    List[Decoder[Payload]](
      Decoder[TradeAlert].widen,
      Decoder[MarketIndex].widen,
      Decoder[PricePrediction].widen
    ).reduceLeft(_ or _)
}
