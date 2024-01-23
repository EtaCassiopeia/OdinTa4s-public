package io.odin.trader

import io.circe.Decoder
import io.odin.binance.client.model.Candlestick
import io.odin.binance.client.model.CandlestickRequest.Interval
import io.odin.binance.stream.SubscribeRequest
import io.odin.binance.stream.model.{Subscribe, WSResponse}
import io.odin.trader.model.CandlestickEvent.Kline

import scala.language.implicitConversions

object model {
  case class CandlestickEvent(eventType: String, eventTime: Long, symbol: String, kline: Kline) extends WSResponse

  object CandlestickEvent {

    case class Kline(
      startTime: Long,
      closeTime: Long,
      symbol: String,
      interval: String,
      firstTradeId: BigDecimal,
      lastTradeId: BigDecimal,
      openPrice: BigDecimal,
      closePrice: BigDecimal,
      highPrice: BigDecimal,
      lowPrice: BigDecimal,
      baseAssetVolume: BigDecimal,
      numberOfTrades: BigDecimal,
      isClosed: Boolean,
      quoteAssetVolume: BigDecimal,
      takerBuyBaseAssetVolume: BigDecimal,
      takerBuyQuoteAssetVolume: BigDecimal,
      ignore: BigDecimal
    )

    object Kline {

      implicit val decoder: Decoder[Kline] =
        Decoder.forProduct17(
          "t",
          "T",
          "s",
          "i",
          "f",
          "L",
          "o",
          "c",
          "h",
          "l",
          "v",
          "n",
          "x",
          "q",
          "V",
          "Q",
          "B"
        )(Kline.apply)
    }

    implicit val decoder: Decoder[CandlestickEvent] =
      Decoder.forProduct4(
        "e",
        "E",
        "s",
        "k"
      )(CandlestickEvent.apply)

    implicit val candleStickSubscriptionRequest: SubscribeRequest[CandlestickEvent] =
      (symbol: String, interval: Interval) => Subscribe(List(s"${symbol.toLowerCase}@kline_${interval.value}"))

    implicit def toCandleStickEvent(candleStickEvent: CandlestickEvent): Candlestick =
      Candlestick(
        candleStickEvent.kline.startTime,
        candleStickEvent.kline.openPrice,
        candleStickEvent.kline.highPrice,
        candleStickEvent.kline.lowPrice,
        candleStickEvent.kline.closePrice,
        candleStickEvent.kline.baseAssetVolume,
        candleStickEvent.kline.closeTime,
        candleStickEvent.kline.quoteAssetVolume,
        candleStickEvent.kline.numberOfTrades,
        candleStickEvent.kline.takerBuyBaseAssetVolume,
        candleStickEvent.kline.takerBuyQuoteAssetVolume,
        candleStickEvent.kline.ignore
      )
  }
}
