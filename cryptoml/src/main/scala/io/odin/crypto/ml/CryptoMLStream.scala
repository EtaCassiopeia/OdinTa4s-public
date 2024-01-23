package io.odin.crypto.ml

import io.circe.parser.decode
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.ws.WebSocket
import sttp.client.{UriContext, basicRequest}
import zio.stream.{Stream, ZStream}
import zio.{Task, UIO, ZIO, ZManaged}

object CryptoMLStream {

  // TODO: Replace with your API key
  private val APIKEY = sys.env.getOrElse("CRYPTO_ML_API_KEY", "<CRYPTO_ML_API_KEY>")

  private def makeWs(): ZManaged[SttpClient, Throwable, WebSocket[Task]] =
    SttpClient
      .openWebsocket(basicRequest.get(uri"wss://api.crypto-ml.com?apiKey=$APIKEY"))
      .map(_.result)
      .toManaged(ws => UIO(ws.close))

  def stream: ZStream[SttpClient, Throwable, Payload] =
    Stream.managed(makeWs()).flatMap { webSocket =>
      Stream.repeatEffect {
        webSocket.receiveText().flatMap {
          case Left(error) =>
            ZIO.succeed(ErrorResponse(error.reason))
          case Right(value) =>
            decode[Payload](value).fold(
              error => ZIO.succeed(ErrorResponse(error.getMessage)),
              event => ZIO.succeed(event)
            )
        }
      }
    }
}
