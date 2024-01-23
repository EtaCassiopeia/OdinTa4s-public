package io.odin.ta4s.backtest

import better.files._
import com.aol.advertising.vulcan.rolling.TimeAndSizeBasedRollingPolicyConfig
import io.circe.syntax.EncoderOps
import io.odin.avro.ZAvroWriter.{avroWriter, avroWriterConfigLayer, _}
import io.odin.avro.{AvroWriteConfig, ZAvroWriter}
import io.odin.binance.client.BinanceClient
import io.odin.binance.client.BinanceClient.{BinanceClient, candleSticks}
import io.odin.binance.client.model.CandlestickRequest.Interval
import io.odin.binance.client.model.{Candlestick, CandlestickRequest}
import io.odin.binance.http.HttpClient.{HttpClient, HttpClientEnv}
import io.odin.binance.http.{HttpClient, HttpClientError}
import io.odin.common.Utils
import sttp.client.SttpBackendOptions
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.logging.{LogFormat, LogLevel, Logging}
import zio.stream.{ZSink, ZStream}
import zio._
import io.circe.generic.auto._
import io.circe.syntax._
import zio.console.putStrLn

import java.nio.file.Paths
import java.time.LocalDateTime
import scala.concurrent.duration.DurationInt

object DumpHistoricalData extends App {

  val symbol = "BTCUSDT"
  //val symbol = "ETHUSDT"
  //val symbol = "XRPUSDT"

  val startDownloadDateTime = "2020-01-01T00:00"
  val endDownloadDateTime = "2021-12-30T00:00"
//  val endDownloadDateTime = ""

  val interval = Interval.ThirtyMinutes

  val path =
    s"/Users/mohsen/Workspace/personal/odin/data-dump/avro-historical-dumps-$symbol-${interval.value}-${Utils.toLocalDateTime(startDownloadDateTime).map(_.toLocalDate.toString).getOrElse("")}"

  path.toFile.createDirectoryIfNotExists(createParents = true)

  val avroConfig: ULayer[Has[AvroWriteConfig]] = avroWriterConfigLayer(
    AvroWriteConfig(
      s"$path/records.avro",
      new TimeAndSizeBasedRollingPolicyConfig().withFileRollingSizeOf(1)
    )
  )

  private lazy val layer = AsyncHttpClientZioBackend.layer(
    SttpBackendOptions(10.seconds, None)
  ) ++ HttpClient.live ++ BinanceClient.live ++ Logging.console(
    logLevel = LogLevel.Info,
    format = LogFormat.ColoredLogFormat()
  ) ++ ((avroConfig >>> avroWriter[Candlestick]) >>> ZAvroWriter.live)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    val startDate = Utils.toLocalDateTime(startDownloadDateTime)
    val endDate = Utils.toLocalDateTime(endDownloadDateTime)

    assert(
      (startDate, endDate) match {
        case (Some(s), Some(e)) => e.isAfter(s)
        case (Some(_), None)    => true
        case _                  => false
      },
      s"End date [$endDate] should be after start date[$startDate]"
    )

    //Save as AVRO
    downloadFromDate(startDate, endDate)
//      .tap(event => putStrLn(event.toString))
      .foreach((candleStick: Candlestick) => write(candleStick))
      .provideCustomLayer(layer)
      .exitCode

    //Save as JSON
//    downloadFromDate(startDate,endDate)
//      .map (candleSticks => candleSticks.asJson.noSpaces + "\n")
//      .map(_.getBytes)
//      .flatMap(bytes => ZStream.fromIterable(bytes) )
//      .run(ZSink.fromFile(Paths.get("/tmp/json-dump-etc.json")))
//      .provideCustomLayer(layer)
//      .exitCode

  }

  def downloadFromDate(
    startDateTime: Option[LocalDateTime],
    endDateTime: Option[LocalDateTime] = None
  ): ZStream[BinanceClient with HttpClient with HttpClientEnv, HttpClientError, Candlestick] =
    candleSticks(
      CandlestickRequest(
        symbol,
        interval,
        startDateTime.map(Utils.localDatetimeToEpoch),
        endDateTime.map(Utils.localDatetimeToEpoch),
        limit = 1000
      )
    )
}
