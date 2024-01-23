package io.odin.telegram

import canoe.api.models.ChatApi
import canoe.api.{TelegramClient => Client, _}
import canoe.models.{CallbackButtonSelected, PrivateChat, Update}
import canoe.models.outgoing.TextContent
import fs2.Pipe
import io.odin.telegram.domain.ChatId
import io.odin.telegram.scenario.CanoeScenarios
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{Task, ZIO}
import cats.syntax.all._
import canoe.syntax._

private[telegram] final case class Canoe(
  scenarios: CanoeScenarios.Service,
  canoeClient: Client[Task]
) extends TelegramClient.Service {

  implicit val canoe: Client[Task] = canoeClient

  def broadcastMessage(receivers: Set[ChatId], message: String): Task[Unit] =
    ZIO
      .foreach(receivers) { chatId =>
        val api = new ChatApi(PrivateChat(chatId.value, None, None, None))
        api.send(TextContent(message))
      }
      .unit

  def answerCallbacks: Pipe[Task, Update, Update] =
    _.evalTap {
      case CallbackButtonSelected(_, query) =>
        query.data match {
          case Some(cbd) =>
            for {
              _ <- query.message.traverse(_.chat.send(cbd))
              _ <- query.finish
            } yield ()
          case _ => Task.unit
        }
      case _ => Task.unit
    }

  override def start: Task[Unit] =
    Bot
      .polling[Task]
      .follow(
        scenarios.start,
        scenarios.help
      )
      .through(answerCallbacks)
      .compile
      .drain
}
