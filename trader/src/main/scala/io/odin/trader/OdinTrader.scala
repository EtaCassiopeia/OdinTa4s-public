package io.odin.trader

import io.odin.binance.client.BinanceFutureClient
import io.odin.binance.client.BinanceFutureClient.{BinanceFutureClient, BinanceFutureClientEnv}
import io.odin.binance.client.model.CandlestickRequest.Interval
import io.odin.binance.client.model.{Candlestick, Symbols}
import io.odin.binance.http.{Credentials, HttpClient, SubAccount}
import io.odin.binance.stream.BinanceMarketStream.marketStream
import io.odin.common.PrettyCaseClass._
import io.odin.common.Utils
import io.odin.ta4s.domain.{Position, Trade, TradingParameters}
import io.odin.ta4s.indicators.OdinClosePriceIndicator
import io.odin.ta4s.strategy._
import io.odin.telegram.TelegramClient.TelegramClient
import io.odin.telegram.domain.ChatId
import io.odin.telegram.{TelegramClient, TelegramSupport}
import io.odin.trader.model.CandlestickEvent
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.logging.{Logging, log}

import scala.concurrent.ExecutionContext.Implicits.global

object OdinTrader extends App with TradingParameters with TelegramSupport with BinanceSupport {

  val defaultChatId = 409455132

  private val interval = Interval.ThirtyMinutes

  val strategyName: String = sys.env.getOrElse("STRATEGY", "<not-set>")
  val subAccountName: String = sys.env.getOrElse("SUB_ACCOUNT", "<not-set>")

  // TODO: Replace with your API key, consider getting them from configuration files or a secrets manager.
  private val subAccounts = Map(
    "adxlongethusdt_virtual@h6v34ncunoemail.com" -> SubAccount(
      Symbols.ETHUSDT,
      ZLayer.succeed(
        Credentials(
          sys.env.getOrElse("ETHUSDT_API_KEY", "<API-KEY>"),
          sys.env.getOrElse("ETHUSDT_SECRET_KEY", "<SECRET-KEY>")
        )
      )
    ),
    "macdonepercent_virtual@hvfx3ndunoemail.com" -> SubAccount(
      Symbols.BTCUSDT,
      ZLayer.succeed(
        Credentials(
          sys.env.getOrElse("BTCUSDT_API_KEY", "<API-KEY>"),
          sys.env.getOrElse("BTCUSDT_SECRET_KEY", "<SECRET-KEY>")
        )
      )
    ),
    "macdonepercentshort_virtual@gmr2p1zlnoemail.com" -> SubAccount(
      Symbols.BTCUSDT,
      ZLayer.succeed(
        Credentials(
          sys.env.getOrElse("BTCUSDT_API_KEY", "<API-KEY>"),
          sys.env.getOrElse("BTCUSDT_SECRET_KEY", "<SECRET-KEY>")
        )
      )
    )
  )

  val subAccount: SubAccount = subAccounts(subAccountName)
  val symbol: Symbols = subAccount.symbol

  private lazy val layer = {
    AsyncHttpClientZioBackend.layer() ++ HttpClient.live ++ BinanceFutureClient.live ++ Logging.console() ++ unsafeRun(
      canoeLayer
    ) ++ subAccount.credentialsLayer
  }

  private val evaluateStrategy: (
    Option[Trade],
    StrategyEvaluationResult
  ) => ZIO[BinanceFutureClient with BinanceFutureClientEnv with TelegramClient, Any, Option[Trade]] = {
    (currentState, strategyEvaluationResult) =>
      (strategyEvaluationResult match {
        case op @ OpenPosition(_, _, _, _, description) =>
          openNewPosition(op)
            .map { opr =>
              val position = if (opr.executedQty > 0) Position.Long else Position.Short

              Some(
                Trade(
                  position,
                  opr.executedQty,
                  entryPrice = opr.price,
                  entryTimestamp = opr.updateTime,
                  buyDescription = List(description)
                )
              )
            }
        case ClosePosition(_, _, _, _, description) =>
          log.info(s"Closing position $description") *> closeExistingPosition().as(Option.empty[Trade])
        case KeepCurrentSate => ZIO.succeed(currentState)
      }).catchAll(e =>
        log.error(e.getMessage) *> TelegramClient
          .broadcastMessage(Set(ChatId(defaultChatId)), e.getMessage)
          .ignore *> ZIO.succeed(currentState)
      )
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    val program = {
      for {
        _ <-
          TelegramClient
            .broadcastMessage(
              Set(ChatId(defaultChatId)),
              s"""OdinTa4s [${AppInfo.appVersion}]-[$strategyName] (${symbol.toString}) is starting
                 | margin level: $defaultLeverage
                 | stop loss: $defaultStopLoss
                 | take profit: $takeProfit""".stripMargin
            )
            .tapError(t => log.error(t.getMessage))
            .ignore
        //Current state ( current position, latest signal to report )
        state <- Ref.make(Option.empty[Trade])
        //Check for existing open positions
        //There is always an empty position in the response
        _ <- log.info("Checking for existing open positions")
        _ <- queryOpenPositions.tap { openPositions =>
          ZIO.when(openPositions.nonEmpty) {
            //We assume that we only have one open position at a time
            val openPosition = openPositions.head
            val position = if (openPosition.positionAmt > 0) Position.Long else Position.Short
            val trade = Trade(
              position,
              openPosition.positionAmt,
              Utils.currentTimestampUTC,
              openPosition.entryPrice,
              buyDescription = List("Existing open position")
            )
            state.set(Some(trade))
          }
        }
        closePriceIndicator50 <- ZIO.effect(
          new OdinClosePriceIndicator(
            symbol.toString,
            "ClosePrice",
            Interval.ThirtyMinutes,
            50
          )
        )
        indicators = List(closePriceIndicator50)
        riskManagementStrategy = new RiskManagementStrategy(defaultStopLoss)
        strategy = (strategyName match {
            case "MACD_ONE_PERCENT_BTC_USDT" => new OdinMACDOnePercentStrategy[Candlestick](closePriceIndicator50)
            case "MACD_ONE_PERCENT_SHORT_BTC_USDT" =>
              new OdinMACDOnePercentShortOptimizedStrategy[Candlestick](closePriceIndicator50)
            case "ADX_LONG_ETH_USDT" => new OdinADXLongOptimizedStrategy[Candlestick](closePriceIndicator50)
          }).andThen(new TakeProfitStrategy(takeProfit))

        stream <-
          marketStream[CandlestickEvent](symbol.toString, interval)
            .collect { case candleStick: CandlestickEvent => candleStick }
            //Evaluate stop-loss
            .tap { event =>
              (for {
                currentState <- state.get
                strategyEvaluationResult <-
                  ZIO.effect(riskManagementStrategy.evaluate(currentState, event.closeTime, event.close))
                updatedState <- evaluateStrategy(currentState, strategyEvaluationResult)
              } yield updatedState).flatMap(s => state.update(_ => s))
            }
            .collect { case candleStick: CandlestickEvent if candleStick.kline.isClosed => candleStick }
            .tap { event =>
              log.info(
                s"Received an event [${Utils.timestampToLocalDateTime(event.eventTime)}] ${event.kline.toStringWithFields}"
              ) *> ZIO.effect(indicators.foreach(_ + event)) *> {
                for {
                  currentState <- state.get
                  _ <- log.info(s"Current state: ${currentState.toStringWithFields}")
                  strategyEvaluationResult <- ZIO.effect(strategy.evaluate(currentState, event.closeTime, event.close))
                  updatedState <- evaluateStrategy(currentState, strategyEvaluationResult)
                  _ <- log.info(s"Updated state: ${updatedState.toStringWithFields}")
                } yield updatedState
              }.flatMap(s => state.update(_ => s))
            }
            .runDrain
      } yield stream
    }

    program
      .foldCauseM(
        cause =>
          log.error(cause.prettyPrint) *> TelegramClient
            .broadcastMessage(Set(ChatId(defaultChatId)), s"$strategyName: ${cause.prettyPrint}")
            .ignore,
        _ => ZIO.unit
      )
      .provideCustomLayer(layer)
      .exitCode
  }
}
