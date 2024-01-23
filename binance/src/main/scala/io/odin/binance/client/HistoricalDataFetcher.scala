package io.odin.binance.client

import io.odin.binance.client.model.{Candlestick, CandlestickRequest}
import io.odin.binance.client.model.CandlestickRequest.Interval

import java.time.{LocalDateTime, ZoneId, ZoneOffset}
import BinanceClient.candleSticks
import io.odin.binance.client.model.Candlestick
import io.odin.binance.client.model.CandlestickRequest.Interval
import io.odin.binance.http.HttpClient
import sttp.client.SttpBackendOptions
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend._
import zio.logging.{LogFormat, LogLevel, Logging}
import zio.stream.ZStream

import scala.concurrent.duration.DurationInt

object HistoricalDataFetcher {

  private lazy val layer =
    AsyncHttpClientZioBackend.layer(
      SttpBackendOptions(10.seconds, None)
    ) ++ HttpClient.live ++ BinanceClient.live ++ Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    )

  def candleStickStream(symbol: String, interval: Interval, count: Int): ZStream[zio.ZEnv, Throwable, Candlestick] =
    candleSticks(
      CandlestickRequest(
        symbol,
        Interval.FiveMinutes,
        Some(
          LocalDateTime
            .now(ZoneId.of(ZoneOffset.UTC.getId))
            .minusMinutes(interval.finiteDuration.mul(count.longValue() + 1).toMinutes)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli
        )
      )
    ).provideCustomLayer(layer)
}
