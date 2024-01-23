package io.odin.telegram.scenario

import canoe.api.{TelegramClient => Client, _}
import zio._

object CanoeScenarios {
  type CanoeScenarios = Has[Service]

  trait Service {
    def start: Scenario[Task, Unit]
    def help: Scenario[Task, Unit]

  }

  type LiveDeps = Has[Client[Task]]
  def live: URLayer[LiveDeps, Has[Service]] =
    ZLayer.fromService[Client[Task], Service] { client =>
      Live(client)
    }
}
