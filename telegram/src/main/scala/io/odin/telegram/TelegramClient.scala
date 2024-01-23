package io.odin.telegram

import canoe.api.{TelegramClient => Client}
import io.odin.telegram.domain.ChatId
import io.odin.telegram.scenario.CanoeScenarios
import io.odin.telegram.scenario.CanoeScenarios.CanoeScenarios
import zio.ZLayer._
import zio._
import zio.macros.accessible

@accessible
object TelegramClient {
  type TelegramClient = Has[Service]

  trait Service {
    def start: Task[Unit]

    def broadcastMessage(receivers: Set[ChatId], message: String): Task[Unit]
  }

  type CanoeDeps = Has[Client[Task]] with CanoeScenarios
  def canoe: URLayer[CanoeDeps, Has[Service]] =
    fromServices[Client[Task], CanoeScenarios.Service, Service] { (client, scenarios) =>
      Canoe(scenarios, client)
    }
}
