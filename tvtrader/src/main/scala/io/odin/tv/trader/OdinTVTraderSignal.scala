package io.odin.tv.trader

import io.odin.binance.http.HttpClient
import io.odin.binance.http.HttpClient.{HttpClient, HttpClientEnv}
import io.odin.telegram.TelegramClient.TelegramClient
import io.odin.telegram.domain.ChatId
import io.odin.telegram.{TelegramClient, TelegramSupport}
import org.http4s.server.Server
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.clock.Clock
import zio.config.getConfig
import zio.logging.{Logging, log}
import zio.{ZLayer, _}

object OdinTVTraderSignal extends App with TelegramSupport {

  private val defaultChatId = 409455132

  type ChatIDLayer = Has[ChatId]

  type AppEnvironment = HttpClient with HttpClientEnv with TelegramClient with Has[ServerConfig] with ChatIDLayer
  type AppTask[A] = RIO[AppEnvironment, A]

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    val httpServerLayer: ZLayer[AppEnvironment, Throwable, Has[Server[AppTask]]] =
      Http4Server.createHttp4sLayer()

    val appLayer = AsyncHttpClientZioBackend.layer() ++ HttpClient.live ++ Logging.console() ++ Clock.live ++ unsafeRun(
      canoeLayer
    ) ++ ServerConfig.configLayer ++ ZLayer.succeed(ChatId(defaultChatId))

    val program: ZIO[Logging with TelegramClient with Has[ServerConfig], Nothing, Unit] = {
      for {
        serverConfig <- getConfig[ServerConfig]
        _ <- log.info(s"Server is starting $serverConfig")
        _ <-
          TelegramClient
            .broadcastMessage(
              Set(ChatId(defaultChatId)),
              s"OdinTVTrader is starting ${AppInfo.appVersion}"
            )
            .tapError(t => log.error(t.getMessage))
            .ignore

        _ <- ZIO.never
      } yield ()
    }

    program
      .foldCauseM(
        cause =>
          log.error(cause.prettyPrint) *> TelegramClient
            .broadcastMessage(Set(ChatId(defaultChatId)), cause.prettyPrint)
            .ignore,
        _ => ZIO.unit
      )
      .provideSomeLayer[ZEnv](appLayer ++ (appLayer >>> httpServerLayer))
      .exitCode
  }
}
