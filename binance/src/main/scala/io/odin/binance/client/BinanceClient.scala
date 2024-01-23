package io.odin.binance.client

import cats.implicits._
import com.softwaremill.quicklens._
import io.odin.binance.OptionEx
import io.odin.binance.client.model.{Candlestick, CandlestickRequest}
import io.odin.binance.http.HttpClient.{HttpClient, HttpClientEnv, get}
import io.odin.binance.http.HttpClientError
import io.odin.binance.http.QueryStringConverter._
import io.odin.common.Utils
import sttp.model.Uri
import zio.logging.log
import zio.stream.ZStream
import zio.{Chunk, Has, ULayer, ZLayer}

import scala.concurrent.duration._

object BinanceClient {
  type BinanceClient = Has[BinanceClient.Service]

  private val baseUrl = Uri("https://api.binance.com")

  trait Service {
    def candleSticks(
      candlestickRequest: CandlestickRequest
    ): ZStream[HttpClient with HttpClientEnv, HttpClientError, Candlestick]
  }

  object Service {
    val live: Service =
      (candlestickRequest: CandlestickRequest) => {

        ZStream.paginateChunkM(candlestickRequest.startTime) { nextPageStartTime =>
          nextPageStartTime.foreach(timestamp =>
            log.info(s"Downloading : ${Utils.timestampToLocalDateTime(timestamp)}")
          )
          val request = candlestickRequest
            .modify(_.startTime)
            .setToIf(nextPageStartTime.isDefined)(nextPageStartTime)
            .modify(_.endTime)
            .setTo(None)

          get[CandlestickRequest, List[Candlestick]](
            baseUrl.path("/api/v3/klines"),
            request
          )
            .map { response =>
              val nextStartTime: Option[Long] = {
                val downloadedLastTimestamp = OptionEx.when(response.nonEmpty)(response.map(_.closeTime).max)

                OptionEx.when(
                  downloadedLastTimestamp.exists(max =>
                    //If an end time has been provided, check if we still need to download more
                    candlestickRequest.endTime.exists { et =>
                      Duration(et - max, MILLISECONDS) >= candlestickRequest.interval.finiteDuration
                    }
                  )
                )(downloadedLastTimestamp.get)
              }

              (
                Chunk.fromIterable(response),
                nextStartTime match {
                  case Some(v) => Some(v).some
                  case _       => None
                }
              )
            }
        }
      }
  }

  val live: ULayer[BinanceClient] =
    ZLayer.succeed(Service.live)

  def candleSticks(
    candlestickRequest: CandlestickRequest
  ): ZStream[BinanceClient with HttpClient with HttpClientEnv, HttpClientError, Candlestick] =
    ZStream.accessStream(_.get.candleSticks(candlestickRequest))
}
