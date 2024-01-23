package io.odin.ml.trader

import io.odin.binance.http.HttpClient
import io.odin.crypto.ml.{CryptoMLStream, Payload}
import io.odin.telegram.domain.ChatId
import io.odin.telegram.{TelegramClient, TelegramSupport}
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.logging.{Logging, log}

object OdinMLTraderSignal extends App with TelegramSupport {

  private val defaultChatId = 409455132

  private lazy val layer =
    AsyncHttpClientZioBackend.layer() ++ HttpClient.live ++ Logging.console() ++ unsafeRun(
      canoeLayer
    )

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val program = {
      for {
        _ <-
          TelegramClient
            .broadcastMessage(
              Set(ChatId(defaultChatId)),
              s"OdinMlTrader [${AppInfo.appVersion}] is starting"
            )
            .tapError(t => log.error(t.getMessage))
            .ignore

        stream <-
          CryptoMLStream.stream
            .collect { case payload: Payload => payload }
            .tap { event =>
              log.info(
                s"Received an event $event"
              )
            }
            .runDrain
      } yield stream

    }

    program
      .foldCauseM(
        cause =>
          log.error(cause.prettyPrint) *> TelegramClient
            .broadcastMessage(Set(ChatId(defaultChatId)), cause.prettyPrint)
            .ignore,
        _ => ZIO.unit
      )
      .provideCustomLayer(layer)
      .exitCode
  }
}
