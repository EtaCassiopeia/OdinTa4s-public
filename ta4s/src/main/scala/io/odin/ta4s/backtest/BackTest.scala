package io.odin.ta4s.backtest

import com.sksamuel.avro4s.AvroInputStream
import io.odin.avro.AvroRecordMeta
import io.odin.binance.client.model.Candlestick
import io.odin.binance.client.model.CandlestickRequest.Interval
import io.odin.common.Utils
import io.odin.ta4s.backtest.strategy._
import io.odin.ta4s.indicators._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.console.{Console, putStrLn}
import zio.logging.{LogFormat, LogLevel, Logging}
import zio.stream.ZStream
import zio._
import io.odin.common.PrettyCaseClass._

import java.io.File
import java.time.ZoneOffset
import scala.concurrent.ExecutionContext.Implicits.global

object BackTest extends App {

  val symbol = "BTCUSDT"
//  val symbol = "ETHUSDT"
  //val symbol = "XRPUSDT"

  val interval = Interval.ThirtyMinutes

  val thirtyMinutesDataDumpPath =
    s"/Users/mohsen/Workspace/personal/odin/data-dump/avro-historical-dumps-$symbol-${interval.value}-2020-01-01"

  val startTimestamp = Utils
    .toLocalDateTime("2020-11-01T00:00")
    .get
    .toInstant(ZoneOffset.UTC)
    .toEpochMilli

  val endTimestamp = Utils
    .toLocalDateTime("2021-12-30T23:59")
    .get
    .toInstant(ZoneOffset.UTC)
    .toEpochMilli

  private lazy val layer = AsyncHttpClientZioBackend.layer() ++ Logging.console(
    logLevel = LogLevel.Info,
    format = LogFormat.ColoredLogFormat()
  )

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {

    def loadFromFile[R, E](filePath: String, startDate: Long, endDate: Long)(
      use: Candlestick => ZIO[R, E, Any]
    ): ZIO[R with Logging with Console, Any, Unit] = {
      ZStream
        .fromIterator(Utils.recursiveListFiles(new File(filePath)).iterator)
        .foreach { file =>
          ZManaged
            .fromAutoCloseable {
              logging.log(LogLevel.Info)(s"Reading ${file.getParent}/${file.getName}") *>
                ZIO.effect(
                  AvroInputStream.data[Candlestick].from(file).build(AvroRecordMeta[Candlestick].schema)
                )
            }
            .use(inputStream =>
              ZStream
                .fromIterator(inputStream.iterator)
                .filter(e => e.openTime >= startDate && e.closeTime <= endDate)
                .foreach(use)
            )
        }
    }

    val program =
      for {
        backTestStatus <- Ref.make(BackTestStatus())

        closedPriceIndicator50 <- ZIO.succeed(
          new OdinClosePriceIndicator(
            symbol,
            "ClosedPrice",
            Interval.ThirtyMinutes,
            50,
            autoInitialize = false
          )
        )

        closedPriceIndicator200 <- ZIO.succeed(
          new OdinClosePriceIndicator(
            symbol,
            "ClosedPrice",
            Interval.ThirtyMinutes,
            200,
            autoInitialize = false
          )
        )

        macdIndicator <- ZIO.succeed(
          new OdinMACDHistogramIndicator(symbol, "MACDHist", interval, autoInitialize = false)
        )
        stochIndicator <- ZIO.succeed(
          new OdinStochasticOscillatorDIndicator(
            symbol,
            "StochasticOscillator",
            interval,
            barCount = 14,
            autoInitialize = false
          )
        )

        indicators = List(closedPriceIndicator50, macdIndicator, stochIndicator, closedPriceIndicator200)
        adxLongOptimizedStrategy = new ADXLongOptimizedStrategy(closedPriceIndicator50)
        adxShortOptimizedStrategy = new ADXShortOptimizedStrategy(closedPriceIndicator50)
        indicatorTestStrategy = new IndicatorTestStrategy(closedPriceIndicator50)
        odinMACDStochasticStrategy = new MACDStochasticStrategyV2(closedPriceIndicator50)
        odinMACDOnePercentStrategy = new MACDOnePercentStrategy(closedPriceIndicator50, 50)
        odinMACDOnePercentShortOptimizedStrategy = new MACDOnePercentShortOptimizedStrategy(closedPriceIndicator50, 50)
        odinVwapMvwapEmaCrossOverStrategy = new VwapMvwapEmaCrossOverStrategy(closedPriceIndicator50, 50)
        odinZigZagStrategy = new ZigZagStrategy(closedPriceIndicator50, 50)
        odinZigZagSimpleStrategy = new ZigZagSimpleStrategy(closedPriceIndicator50, 50)
        odinSqueezeMomentumStrategy = new SqueezeMomentumStrategy(closedPriceIndicator50, 50)
        odinChaikinSqueezeMomentumStrategy = new ChaikinSqueezeMomentumStrategy(closedPriceIndicator50, 50)
        thirtyMinutesIOStream <- loadFromFile(thirtyMinutesDataDumpPath, startTimestamp, endTimestamp) { event =>
          //putStrLn(s"[${Utils.timestampToLocalDateTime(event.closeTime)}] ${event.toStringWithFields}") *>
          ZIO.effect(
            indicators.foreach(_ + event)
          ) *>
            backTestStatus
              .update(status =>
                //MACDStochasticStrategy
                //.evaluate(status, macdIndicator, stochIndicator, event.close, event.closeTime)
                //odinMACDStochasticStrategy.evaluate(status, event.close, event.closeTime)
                //adxLongOptimizedStrategy.evaluate(status, event.close, event.closeTime)
                //adxShortOptimizedStrategy.evaluate(status, event.close, event.closeTime)
                //indicatorTestStrategy.evaluate(status, event.close, event.closeTime)
                //odinMACDOnePercentStrategy.evaluate(status, event.close, event.closeTime)
                //odinMACDOnePercentShortOptimizedStrategy.evaluate(status, event.close, event.closeTime)
                //odinVwapMvwapEmaCrossOverStrategy.evaluate(status, event.close, event.closeTime)
                //odinZigZagStrategy.evaluate(status, event.close, event.closeTime)
                //odinZigZagSimpleStrategy.evaluate(status, event.close, event.closeTime)
                //odinSqueezeMomentumStrategy.evaluate(status, event.close, event.closeTime)
                odinChaikinSqueezeMomentumStrategy.evaluate(status, event.close, event.closeTime)
              )
        }

        finalStatus <- backTestStatus.get
        _ <- putStrLn(finalStatus.toString)
      } yield thirtyMinutesIOStream

    program.provideCustomLayer(layer).exitCode
  }
}
