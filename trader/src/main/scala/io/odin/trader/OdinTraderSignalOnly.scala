//package io.odin.trader
//
//import canoe.api.{TelegramClient => CanoeClient}
//import io.odin.binance.client.model.CandlestickRequest.Interval
//import io.odin.binance.client.model.{Assets, Candlestick, Symbols}
//import io.odin.binance.stream.BinanceMarketStream.marketStream
//import io.odin.common.PrettyCaseClass._
//import io.odin.common.Utils
//import io.odin.ta4s.domain.{Trade, TradingParameters}
//import io.odin.ta4s.indicators.OdinClosePriceIndicator
//import io.odin.ta4s.strategy._
//import io.odin.telegram.TelegramClient
//import io.odin.telegram.domain.ChatId
//import io.odin.telegram.domain.TelegramError.MissingBotToken
//import io.odin.telegram.scenario.CanoeScenarios
//import io.odin.trader.model.CandlestickEvent
//import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
//import zio._
//import zio.console.putStrLn
//import zio.interop.catz._
//import zio.logging.{LogFormat, LogLevel, Logging, _}
//import zio.system.System
//
//import scala.concurrent.ExecutionContext.Implicits.global
//
//object OdinTraderSignalOnly extends App with TradingParameters {
//
//  private val symbol = Symbols.BTCUSDT.toString
//  private val sourceAsset = Assets.USDT
//  private val destAsset = Assets.BTC
//
//  private val interval = Interval.ThirtyMinutes
//  private val defaultChatId = 409455132
//
//  private def makeCanoeLayer(canoeClient: TaskManaged[CanoeClient[Task]]) = {
//    val canoeClientLayer = canoeClient.toLayer.orDie
//    val canoeScenarioLayer = canoeClientLayer >>> CanoeScenarios.live
//    (canoeScenarioLayer ++ canoeClientLayer) >>> TelegramClient.canoe
//  }
//
//  private lazy val layer = {
//    val canoeLayer = for {
//      token <- telegramBotToken.orElse(UIO.succeed("1455269967:AAG3xFGJjrlkYA_8ok2poWnxj0emQkWq7TY"))
//      canoeClient <- makeCanoeClient(token)
//    } yield makeCanoeLayer(canoeClient)
//
//    AsyncHttpClientZioBackend.layer() ++ Logging.console(
//      logLevel = LogLevel.Info,
//      format = LogFormat.ColoredLogFormat()
//    ) ++ unsafeRun(canoeLayer)
//  }
//
//  private def telegramBotToken: RIO[System, String] =
//    for {
//      token <- system.env("BOT_TOKEN")
//      token <- ZIO.fromOption(token).orElseFail(MissingBotToken)
//    } yield token
//
//  private def makeCanoeClient(token: String): UIO[TaskManaged[CanoeClient[Task]]] =
//    ZIO
//      .runtime[Any]
//      .map { implicit rts =>
//        CanoeClient
//          .global[Task](token)
//          .toManaged
//      }
//
//  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
//    val program = {
//      for {
//        //Current state ( current position, latest signal to report )
//        state <- Ref.make((Option.empty[Trade], Option.empty[StrategyEvaluationResult]))
//        closePriceIndicator50 <- ZIO.succeed(
//          new OdinClosePriceIndicator(
//            symbol,
//            "ClosePrice",
//            Interval.ThirtyMinutes,
//            50
//          )
//        )
//        indicators = List(closePriceIndicator50)
//        riskManagementStrategy = new RiskManagementStrategy(defaultStopLoss)
//        macdOnePercentStrategy = new OdinMACDOnePercentStrategy[Candlestick](closePriceIndicator50)
//        stream <-
//          marketStream[CandlestickEvent](symbol, interval)
////          .tap {
////            case candleStick: CandlestickEvent => putStrLn(s"Received an event [${Utils.timestampToLocalDateTime(candleStick.eventTime)}] ${candleStick.kline.toStringWithFields}")
////            case _ => ZIO.unit
////          }
//            .collect { case candleStick: CandlestickEvent if candleStick.kline.isClosed => candleStick }
//            .tap { event =>
//              log.info(
//                s"Received an event [${Utils.timestampToLocalDateTime(event.eventTime)}] ${event.kline.toStringWithFields}"
//              ) *> ZIO.effect(indicators.foreach(_ + event)) *>
//                state.update {
//                  case (openPosition, _) =>
//                    val riskManagementStrategyResult =
//                      riskManagementStrategy.evaluate(openPosition, event.closeTime, event.close)
//                    riskManagementStrategyResult match {
//                      case cp @ ClosePosition(_, _, price, timestamp, description) =>
//                        val updateOpenPosition = openPosition.map(t =>
//                          t.copy(
//                            exitPrice = price,
//                            exitTimestamp = timestamp,
//                            soldOnStopLoss = true,
//                            description = List(description)
//                          )
//                        )
//                        (None, Some(cp))
//                      case _ =>
//                        val strategyEvaluationResult =
//                          macdOnePercentStrategy.evaluate(openPosition, event.closeTime, event.close)
//                        strategyEvaluationResult match {
//                          case op @ OpenPosition(_, position, price, timestamp, description) =>
//                            (
//                              Some(
//                                Trade(
//                                  position,
//                                  BigDecimal(1),
//                                  entryPrice = price,
//                                  entryTimestamp = timestamp,
//                                  description = List(description)
//                                )
//                              ),
//                              Some(op)
//                            )
//                          case cp @ ClosePosition(_, _, price, timestamp, description) =>
//                            val updateOpenPosition = openPosition.map(t =>
//                              t.copy(exitPrice = price, exitTimestamp = timestamp, description = List(description))
//                            )
//                            (None, Some(cp))
//                          case KeepCurrentSate => (openPosition, None)
//                        }
//                    }
//                } *> state.get.flatMap {
//                case (_, Some(newEvent)) =>
//                  log.info {
//                    newEvent match {
//                      case op: OpenPosition  => op.toStringWithFields
//                      case cp: ClosePosition => cp.toStringWithFields
//                      case _                 => "Keep state"
//                    }
//                  } *>
//                    TelegramClient.broadcastMessage(Set(ChatId(defaultChatId)), newEvent.toString).unit
//                case _ => ZIO.unit
//              }
//            }
//            .runDrain
//      } yield stream
//    }
//
//    program
//      .foldCauseM(
//        cause =>
//          putStrLn(cause.prettyPrint) *> TelegramClient.broadcastMessage(Set(ChatId(defaultChatId)), cause.prettyPrint),
//        _ => ZIO.unit
//      )
//      .provideCustomLayer(layer)
//      .exitCode
//  }
//}
