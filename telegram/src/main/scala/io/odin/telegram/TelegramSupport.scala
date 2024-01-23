package io.odin.telegram

import io.odin.telegram.domain.TelegramError.MissingBotToken
import io.odin.telegram.scenario.CanoeScenarios
import zio.system.System
import zio.{RIO, Task, TaskManaged, UIO, ZIO, system}
import canoe.api.{TelegramClient => CanoeClient}
import zio.interop.catz._

trait TelegramSupport {
  private def makeCanoeLayer(canoeClient: TaskManaged[CanoeClient[Task]]) = {
    val canoeClientLayer = canoeClient.toLayer.orDie
    val canoeScenarioLayer = canoeClientLayer >>> CanoeScenarios.live
    (canoeScenarioLayer ++ canoeClientLayer) >>> TelegramClient.canoe
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

  val canoeLayer = for {
    token <- telegramBotToken.orElse(UIO.succeed("1455269967:AAG3xFGJjrlkYA_8ok2poWnxj0emQkWq7TY"))
    canoeClient <- makeCanoeClient(token)
  } yield makeCanoeLayer(canoeClient)
}
