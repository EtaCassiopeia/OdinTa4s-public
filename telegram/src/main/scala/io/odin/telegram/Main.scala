package io.odin.telegram

import canoe.api.{TelegramClient => CanoeClient}
import io.odin.telegram.domain.TelegramError.MissingBotToken
import io.odin.telegram.scenario.CanoeScenarios
import io.odin.telegram.domain.ChatId
import zio._
import zio.console.putStrLn
import zio.interop.catz._
import zio.system._

object Main extends zio.App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val program = for {
      // TODO Replace with your Telegram bot token read from environment variable or config file
      token <- telegramBotToken.orElse(UIO.succeed("<TELEGRAM_TOKEN>"))
      canoeClient <- makeCanoeClient(token)
      _ <- TelegramClient.start.provideSomeLayer(layer(canoeClient))
    } yield ()

    program.foldM(
      err => putStrLn(s"Execution failed with: ${err.getMessage}") *> ZIO.succeed(ExitCode.failure),
      _ => ZIO.succeed(ExitCode.success)
    )
  }

  private def telegramBotToken: RIO[System, String] =
    for {
      token <- system.env("BOT_TOKEN")
      token <- ZIO.fromOption(token).orElseFail(MissingBotToken)
    } yield token

  private def makeCanoeClient(token: String): UIO[TaskManaged[CanoeClient[Task]]] =
    ZIO
      .runtime[Any]
      .map { implicit rts =>
        CanoeClient
          .global[Task](token)
          .toManaged
      }

  private def layer(canoeClient: TaskManaged[CanoeClient[Task]]) = {

    val canoeClientLayer = canoeClient.toLayer.orDie

    val canoeScenarioLayer = canoeClientLayer >>> CanoeScenarios.live

    val telegramClientLayer = (canoeScenarioLayer ++ canoeClientLayer) >>> TelegramClient.canoe

    telegramClientLayer
  }

}
