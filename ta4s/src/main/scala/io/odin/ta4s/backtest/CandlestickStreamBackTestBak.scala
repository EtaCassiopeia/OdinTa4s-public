//package io.odin.ta4s.backtest
//
//import com.sksamuel.avro4s.AvroInputStream
//import io.odin.ta4s.avro.AvroRecordMeta
//import io.odin.ta4s.avro.AvroRecordMeta._
//import io.odin.ta4s.backtest.chart.{ComboChartSaver, CrossChartBuilder, LineChartBuilder}
//import io.odin.ta4s.backtest.common.Utils
//import io.odin.ta4s.backtest.strategy._
//import io.odin.ta4s.binance.client.model.Candlestick
//import io.odin.ta4s.binance.client.model.CandlestickRequest.Interval
//import io.odin.ta4s.indicators._
//import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
//import zio.console.{Console, putStrLn}
//import zio.logging.{LogFormat, LogLevel, Logging}
//import zio.stream.ZStream
//import zio.{App, ExitCode, Ref, ZIO, ZManaged, logging}
//
//import java.io.File
//import java.time.ZoneOffset
//import scala.concurrent.ExecutionContext.Implicits.global
//
//object CandlestickStreamBackTestBak extends App {
//
//  val symbol = "BTCUSDT"
////    val symbol = "ETHUSDT"
//  //val symbol = "XRPUSDT"
//
//  val thirtyMinutesInterval = Interval.ThirtyMinutes
//  val oneMinutesInterval = Interval.OneMinute
//
//  val thirtyMinutesDataDumpPath =
//    s"/Users/mohsen/Workspace/personal/odin/data-dump/avro-historical-dumps-$symbol-${thirtyMinutesInterval.value}-2020-01-01"
//  val oneMinutesDataDumpPath =
//    s"/Users/mohsen/Workspace/personal/odin/data-dump/avro-historical-dumps-$symbol-${oneMinutesInterval.value}-2020-11-01"
//
//  val startTimestamp = Utils
//    .toLocalDateTime("2020-01-01T00:00")
//    .get
//    .toInstant(ZoneOffset.UTC)
//    .toEpochMilli
//
//  val endTimestamp = Utils
//    .toLocalDateTime("2020-12-30T23:59")
//    .get
//    .toInstant(ZoneOffset.UTC)
//    .toEpochMilli
//
//  private lazy val layer = AsyncHttpClientZioBackend.layer() ++ Logging.console(
//    logLevel = LogLevel.Info,
//    format = LogFormat.ColoredLogFormat()
//  )
//
//  private def withChartBuilders =
//    for {
//      mvapChart <-
//        ZManaged
//          .fromAutoCloseable(
//            ZIO.effect(new LineChartBuilder[Candlestick]("MVWAP", c => c.close.doubleValue(), autoSave = false))
//          )
//      macdChart <- ZManaged.fromAutoCloseable(
//        ZIO.effect(
//          new CrossChartBuilder[OdinMACDHistogramIndicator[Candlestick]](
//            "MACD",
//            macd => macd.getMACDLine,
//            macd => macd.getSignal,
//            autoSave = false
//          )
//        )
//      )
//      dailyHighChart <- ZManaged.fromAutoCloseable(
//        ZIO.effect(
//          new LineChartBuilder[OdinDailyHighIndicator[Candlestick]](
//            "DailyHigh",
//            c => c.value.getOrElse(0),
//            autoSave = false
//          )
//        )
//      )
//
//      dailyLowChart <- ZManaged.fromAutoCloseable(
//        ZIO.effect(
//          new LineChartBuilder[OdinDailyLowIndicator[Candlestick]](
//            "DailyLow",
//            c => c.value.getOrElse(0),
//            autoSave = false
//          )
//        )
//      )
//    } yield ((mvapChart, macdChart, dailyHighChart, dailyLowChart))
//
//  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
//
//    def loadFromFile[R, E](filePath: String, startDate: Long, endDate: Long)(
//      use: Candlestick => ZIO[R, E, Any]
//    ): ZIO[R with Logging with Console, Any, Unit] = {
//      ZStream
//        .fromIterator(Utils.recursiveListFiles(new File(filePath)).iterator)
//        .foreach { file =>
//          ZManaged
//            .fromAutoCloseable {
//              logging.log(LogLevel.Info)(s"Reading ${file.getParent}/${file.getName}") *>
//                ZIO.effect(
//                  AvroInputStream.data[Candlestick].from(file).build(AvroRecordMeta[Candlestick].schema)
//                )
//            }
//            .use(inputStream =>
//              ZStream
//                .fromIterator(inputStream.iterator)
//                .filter(e => e.openTime >= startDate && e.closeTime <= endDate)
//                .foreach(use)
//            )
//        }
//    }
//
//    val program = withChartBuilders
//      .use {
//        case (mvwapLineChart, macdCrossLineChart, dailyHighChart, dailyLowChart) =>
//          ZManaged
//            .fromAutoCloseable(
//              ZIO.effect(
//                new ComboChartSaver(
//                  "ComboChart",
//                  "/tmp",
//                  mvwapLineChart,
//                  macdCrossLineChart,
//                  dailyHighChart,
//                  dailyLowChart
//                )
//              )
//            )
//            .use { comboChartSaver =>
//              for {
//                backTestStatus <- Ref.make(BackTestStatus())
//                mvwapIndicator <- ZIO.succeed(
//                  // 7 results in the highest success rate, 6 results in the highest profit
//                  new OdinMVWAPIndicator(symbol, "MVWAP", thirtyMinutesInterval, 7, autoInitialize = false)
//                )
//                macdIndicator <- ZIO.succeed(
//                  new OdinMACDHistogramIndicator(symbol, "MACDHist", thirtyMinutesInterval, autoInitialize = false)
//                )
//                stochIndicator <- ZIO.succeed(
//                  new OdinStochasticOscillatorDIndicator(
//                    symbol,
//                    "StochasticOscillator",
//                    thirtyMinutesInterval,
//                    barCount = 14,
//                    autoInitialize = false
//                  )
//                )
//                trendIndicator <- ZIO.succeed(
//                  new OdinParabolicSarIndicator(
//                    symbol,
//                    "TrendIndicator",
//                    thirtyMinutesInterval,
//                    24 * 2, //one bar for each half an hour
//                    autoInitialize = false
//                  )
//                )
//                macd <-
//                  ZIO.succeed(new OdinMACDIndicator("BTCUSDT", "MACD", Interval.ThirtyMinutes, autoInitialize = false))
//                emaMacd <- ZIO.succeed(
//                  new OdinEMAMACDIndicator("BTCUSDT", "MEAMACD", Interval.ThirtyMinutes, autoInitialize = false)
//                )
//                ema7 <- ZIO.succeed(
//                  new OdinEMAIndicator("BTCUSDT", "EMA7", Interval.ThirtyMinutes, 7, autoInitialize = false)
//                )
//                ema25 <- ZIO.succeed(
//                  new OdinEMAIndicator("BTCUSDT", "EMA25", Interval.ThirtyMinutes, 25, autoInitialize = false)
//                )
//                ema99 <- ZIO.succeed(
//                  new OdinEMAIndicator("BTCUSDT", "EMA99", Interval.ThirtyMinutes, 99, autoInitialize = false)
//                )
//                closedPriceIndicator50 <- ZIO.succeed(
//                  new OdinClosePriceIndicator(
//                    "BTCUSDT",
//                    "ClosedPrice",
//                    Interval.ThirtyMinutes,
//                    50,
//                    autoInitialize = false
//                  )
//                )
//                closedPriceIndicator200 <- ZIO.succeed(
//                  new OdinClosePriceIndicator(
//                    "BTCUSDT",
//                    "ClosedPrice",
//                    Interval.ThirtyMinutes,
//                    200,
//                    autoInitialize = false
//                  )
//                )
//                closedPriceIndicator99 <- ZIO.succeed(
//                  new OdinClosePriceIndicator(
//                    "BTCUSDT",
//                    "ClosedPrice",
//                    Interval.ThirtyMinutes,
//                    99,
//                    autoInitialize = false
//                  )
//                )
//                openPriceIndicator99 <- ZIO.succeed(
//                  new OdinOpenPriceIndicator(
//                    "BTCUSDT",
//                    "ClosedPrice",
//                    Interval.ThirtyMinutes,
//                    99,
//                    autoInitialize = false
//                  )
//                )
//                openPriceIndicator50 <- ZIO.succeed(
//                  new OdinOpenPriceIndicator(
//                    "BTCUSDT",
//                    "ClosedPrice",
//                    Interval.ThirtyMinutes,
//                    99,
//                    autoInitialize = false
//                  )
//                )
//                stochKIndicator <- ZIO.succeed(
//                  new OdinStochasticOscillatorKIndicator(
//                    symbol,
//                    "StochasticOscillatorK",
//                    thirtyMinutesInterval,
//                    barCount = 14,
//                    autoInitialize = false
//                  )
//                )
//                dailyHigh <- ZIO.succeed(new OdinDailyHighIndicator(thirtyMinutesInterval))
//                dailyLow <- ZIO.succeed(new OdinDailyLowIndicator(thirtyMinutesInterval))
//                indicators = List(
//                  ema7,
//                  ema25,
//                  ema99,
//                  macd,
//                  emaMacd,
//                  stochKIndicator,
//                  mvwapIndicator,
//                  macdIndicator,
//                  trendIndicator,
//                  dailyHigh,
//                  dailyLow,
//                  stochIndicator,
//                  closedPriceIndicator50,
//                  closedPriceIndicator200,
//                  closedPriceIndicator99,
//                  openPriceIndicator99
//                )
//                adxStrategy = new ADXStrategy(closedPriceIndicator50)
//                adxLongOptimizedStrategy = new ADXLongOptimizedStrategy(closedPriceIndicator50)
//                adxShortOptimizedStrategy = new ADXShortOptimizedStrategy(closedPriceIndicator50)
//                cciCorrectionStrategy = new CCICorrectionStrategy(closedPriceIndicator200)
//                globalExtremaStrategy = new GlobalExtremaStrategy(closedPriceIndicator50)
//                rsi2Strategy = new RSI2Strategy(closedPriceIndicator200)
//                movingMomentumStrategy = new MovingMomentumStrategy(closedPriceIndicator200)
//                longTermMovingMomentumStrategy = new LongTermMovingMomentumStrategy(closedPriceIndicator50)
//                indicatorTestStrategy = new IndicatorTestStrategy(closedPriceIndicator50)
//                thirtyMinutesIOStream <- loadFromFile(thirtyMinutesDataDumpPath, startTimestamp, endTimestamp) {
//                  event =>
//                    //Load and apply corresponding one minute data
//                    //                    loadFromFile(oneMinutesDataDumpPath, event.openTime, event.closeTime) { candleStick =>
//                    //                      ZIO.effect(trendIndicator + candleStick) *> ZIO.fromEither(trendIndicator.value) //Read the result to make sure it calculates the value for all of the input data
//                    //                    } *>
//                    //                      putStrLn(s"${Utils.timestampToLocalDateTime(event.openTime)}: ${trendIndicator.marketTrend}") *>
//                    ZIO.effect(indicators.foreach(_ + event)) *>
//                      //Call the save method before adding new elements to the charts to clear the previous buffer
//                      //ZIO.effect(comboChartSaver.checkAndSave(event.openTime)) *>
//                      //ZIO.effect(mvwapLineChart.addEvent(event)) *>
//                      //ZIO.effect(macdCrossLineChart.addEvent(macdIndicator)) *>
//                      //ZIO.effect(dailyHighChart.addEvent(dailyHigh)) *>
//                      //ZIO.effect(dailyLowChart.addEvent(dailyLow)) *>
//                      backTestStatus
//                        .update(status =>
////                          MACDCrossOverStrategy.evaluate(status, macdIndicator, event.close, event.openTime)
////                          MACDStochasticStrategy
////                            .evaluate(status, macdIndicator, stochIndicator, event.close, event.openTime)
////                          MACDStochasticReverseStrategy
////                            .evaluate(status, macdIndicator, stochIndicator, event.close, event.openTime)
////                          StochasticStrategy
////                            .evaluate(status, stochIndicator, event.close, event.openTime)
////                          SimpleMovingAverageStrategy.evaluate(status, sma, event.close, event.openTime)
////                          MovingVolumeWeightedAveragePriceStrategy
////                            .evaluate(status, mvwapIndicator, event.close, event.openTime)
////                          DailyMovementsStrategy.evaluate(status, dailyHigh, dailyLow, event.close, event.openTime)
////                          adxStrategy.evaluate(status,event.close, event.openTime)
////                          adxLongOptimizedStrategy.evaluate(status, event.close, event.openTime)
//                          adxShortOptimizedStrategy.evaluate(status, event.close, event.openTime)
////                          indicatorTestStrategy.evaluate(status, event.close, event.openTime)
////                          cciCorrectionStrategy.evaluate(status,event.close, event.openTime)
////                          globalExtremaStrategy.evaluate(status,event.close, event.openTime)
////                            rsi2Strategy.evaluate(status,event.close, event.openTime)
////                              movingMomentumStrategy.evaluate(status,event.close, event.openTime)
////                          longTermMovingMomentumStrategy.evaluate(status,event.close, event.openTime)
//                        )
//                }
//
//                finalStatus <- backTestStatus.get
//                _ <- putStrLn(finalStatus.toString)
//              } yield thirtyMinutesIOStream
//            }
//      }
//
//    program.provideCustomLayer(layer).exitCode
//  }
//}
