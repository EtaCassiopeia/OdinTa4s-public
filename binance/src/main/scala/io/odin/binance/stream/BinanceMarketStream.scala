package io.odin.binance.stream

import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.odin.binance.client.model.CandlestickRequest.Interval
import io.odin.binance.stream.model.{ErrorResponse, WSResponse}
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.basicRequest
import sttp.client.ws.WebSocket
import sttp.model.Uri._
import sttp.model.ws.WebSocketFrame
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.duration._
import zio.logging.{Logging, log}
import zio.stream._

object BinanceMarketStream {

  private def makeWs(): ZManaged[SttpClient, Throwable, WebSocket[Task]] =
    SttpClient
      .openWebsocket(basicRequest.get(uri"wss://fstream.binance.com/ws"))
      .map(_.result)
      .toManaged(ws => UIO(ws.close))

  def marketStream[T <: WSResponse: SubscribeRequest: Decoder](
    symbol: String,
    interval: Interval
  ): ZStream[Console with Clock with SttpClient with Logging, Throwable, Any] =
    Stream.managed(makeWs()).flatMap { ws =>
      Stream
        .fromEffect(
          ws.send(WebSocketFrame.text(SubscribeRequest[T].request(symbol, interval).asJson.noSpaces)) *> ws
            .receiveText()
            .tapBoth(e => log.error(e.getMessage), r => log.info(r.toString))
        ) *> (Stream
        .repeatEffectWith(ws.send(WebSocketFrame.pong), Schedule.spaced(5.minute))
        .mergeTerminateEither(Stream.repeatEffect {
          ws.receiveText().flatMap {
            case Left(error) =>
              ZIO.succeed(ErrorResponse(error.reason))
            case Right(value) =>
              decode[T](value).fold(
                error => ZIO.succeed(ErrorResponse(error.getMessage)),
                event => ZIO.succeed(event)
              )
          }
        }))
    }
}
