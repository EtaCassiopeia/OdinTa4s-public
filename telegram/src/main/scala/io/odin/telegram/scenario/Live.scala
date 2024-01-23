package io.odin.telegram.scenario

import canoe.api._
import canoe.api.models.Keyboard
import canoe.models.{Chat, InlineKeyboardButton, InlineKeyboardMarkup}
import canoe.models.messages.TextMessage
import canoe.syntax._
import zio._

private[scenario] final case class Live(
  canoeClient: TelegramClient[Task]
) extends CanoeScenarios.Service {

  private implicit val client: TelegramClient[Task] = canoeClient

  val inlineBtn = InlineKeyboardButton.callbackData(text = "button", cbd = "callback data")

  //val inlineKeyboardMarkUp = InlineKeyboardMarkup.singleButton(inlineBtn)
  val inlineKeyboardMarkUp = InlineKeyboardMarkup.singleColumn(Seq(inlineBtn, inlineBtn))
  val keyboardMarkUp = Keyboard.Inline(inlineKeyboardMarkUp)

  override def help: Scenario[Task, Unit] =
    for {
      chat <- Scenario.expect(command("help").chat)
      _ <- broadcastHelp(chat)
    } yield ()

  override def start: Scenario[Task, Unit] =
    for {
      chat <- Scenario.expect(command("start").chat)
      _ <- Scenario.eval(chat.send(content = "pretty message", keyboard = keyboardMarkUp))
      _ <- broadcastHelp(chat)
    } yield ()

  private def broadcastHelp(chat: Chat): Scenario[Task, TextMessage] = {
    val helpText =
      s"""chatId ${chat.id}
         |/help Shows help menu
         |""".stripMargin

    Scenario.eval(chat.send(helpText))
  }
}
