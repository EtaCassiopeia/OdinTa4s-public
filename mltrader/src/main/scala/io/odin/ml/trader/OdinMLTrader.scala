package io.odin.ml.trader

import io.odin.binance.client.BinanceFutureClient
import io.odin.binance.client.BinanceFutureClient.{BinanceFutureClient, BinanceFutureClientBaseEnv}
import io.odin.binance.client.model.Symbols
import io.odin.binance.http.{Credentials, HttpClient, SubAccount}
import io.odin.common.PrettyCaseClass._
import io.odin.crypto.ml.Payload.OrderSide
import io.odin.crypto.ml.{CryptoMLStream, TradeAlert}
import io.odin.telegram.TelegramClient.TelegramClient
import io.odin.telegram.domain.ChatId
import io.odin.telegram.{TelegramClient, TelegramSupport}
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.logging.{Logging, log}

object OdinMLTrader extends App with TelegramSupport with BinanceSupport {

  override val defaultChatId: Long = 409455132
  // TODO: Replace with your API key
  private val subAccounts = Map(
    "BTC-USD" -> SubAccount(
      Symbols.BTCUSDT,
      ZLayer.succeed(
        Credentials(
          "<CREDENTIALS>",
          "<CREDENTIALS>"
        )
      )
    ),
    "ETH-USD" -> SubAccount(
      Symbols.ETHUSDT,
      ZLayer.succeed(
        Credentials(
          "<CREDENTIALS>",
          "<CREDENTIALS>"
        )
      )
    )
  )

  private lazy val layer =
    AsyncHttpClientZioBackend.layer() ++ HttpClient.live ++ BinanceFutureClient.live ++ Logging.console() ++ unsafeRun(
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
            .collect { case tradeAlert: TradeAlert => tradeAlert }
            .tap { tradeAlert =>
              log.info(
                s"Received an event ${tradeAlert.toStringWithFields}"
              ) *> (tradeAlert match {
                //TODO The buy alert has more information that might be useful to create a proper order
                case TradeAlert(OrderSide.buy, productCode, price, _, bot, _, _, _, _) =>
                  subAccounts.get(productCode) match {
                    case Some(subAccount) =>
                      openNewPosition(subAccount.symbol, price)
                        .provideSomeLayer[BinanceFutureClient with BinanceFutureClientBaseEnv with TelegramClient](
                          subAccount.credentialsLayer
                        )
                    case _ => log.info("Ignoring a Base Crypto Strategy").unit
                  }

                case TradeAlert(OrderSide.sell, productCode, _, _, bot, _, _, _, _) =>
                  subAccounts.get(productCode) match {
                    case Some(subAccount) =>
                      closeExistingPosition(subAccount.symbol)
                        .provideSomeLayer[BinanceFutureClient with BinanceFutureClientBaseEnv with TelegramClient](
                          subAccount.credentialsLayer
                        )
                    case _ => log.info("Ignoring a Base Crypto Strategy").unit
                  }

                case _ => ZIO.unit
              }).catchAll(e =>
                log.error(e.getMessage) *> TelegramClient
                  .broadcastMessage(Set(ChatId(defaultChatId)), e.getMessage)
                  .ignore
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
