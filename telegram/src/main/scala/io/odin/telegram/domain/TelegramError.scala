package io.odin.telegram.domain

sealed trait TelegramError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object TelegramError {

  final case object MissingBotToken extends TelegramError {
    def message: String = "Bot token is not set as environment variable"
  }

  final case class UnexpectedError(text: String) extends TelegramError {
    def message: String = text
  }

}
