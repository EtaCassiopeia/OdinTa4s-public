package io.odin.tv.trader

import cats.data.Kleisli
import io.circe.Encoder
import io.circe.generic.auto._
import io.odin.telegram.TelegramClient
import io.odin.tv.trader.OdinTVTraderSignal.{AppTask, ChatIDLayer}
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import zio.ZIO
import zio.interop.catz._
import zio.logging.log

object Routes {

  private val dsl = Http4sDsl[AppTask]
  import dsl._

  private implicit def encoder[A](implicit D: Encoder[A]): EntityEncoder[AppTask, A] = jsonEncoderOf[AppTask, A]

  def webhookService(): Kleisli[AppTask, Request[AppTask], Response[AppTask]] =
    HttpRoutes
      .of[AppTask] {
        case req @ POST -> Root / "signal" =>
          for {
            chatId <- ZIO.environment[ChatIDLayer]
            action <- req.as[ActionRequest]
            _ <- log.info(s"Handling request $action")
            _ <-
              TelegramClient
                .broadcastMessage(
                  Set(chatId.get),
                  s"Handling request $action"
                )
                .tapError(t => log.error(t.getMessage))
                .ignore
            resp <- Ok(action.toString)
          } yield resp
      }
      .orNotFound
}
