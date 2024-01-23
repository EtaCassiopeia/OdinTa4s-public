package io.odin.ml.trader

import io.odin.binance.client.BinanceFutureClient._
import io.odin.binance.client.model.Enums.OrderSide
import io.odin.binance.client.model.Requests.{NewMarketOrderRequest, RequestBySymbol}
import io.odin.binance.client.model.{Responses, Symbols}
import io.odin.binance.http.{GenericHttpError, HttpClientError}
import io.odin.common.PrettyCaseClass._
import io.odin.ta4s.domain.TradingParameters
import io.odin.telegram.TelegramClient
import io.odin.telegram.TelegramClient.TelegramClient
import io.odin.telegram.domain.ChatId
import zio._
import zio.logging.log

import scala.math.BigDecimal.RoundingMode
import scala.util.Try

trait BinanceSupport extends TradingParameters {

  protected val defaultChatId: Long

  protected def queryOpenPositions(
    symbol: Symbols
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, List[Responses.OpenPosition]] =
    positionInfo(RequestBySymbol(symbol.toString))
      .map(_.filter(r => r.positionAmt != 0))
      .tapBoth(
        e => log.error(e.getMessage),
        positions => log.info(s"Open positions: ${positions.map(_.toStringWithFields).mkString(",")}")
      )

  protected def openNewPosition(symbol: Symbols, currentPrice: BigDecimal): ZIO[
    BinanceFutureClient with BinanceFutureClientEnv with TelegramClient,
    HttpClientError,
    Responses.OrderPlacedResponse
  ] =
    for {
      sourceAssetBalance <- futuresAccountBalance()
        .map(_.filter(r => r.asset == symbol.sourceAsset.toString))
        .tapBoth(
          e => log.error(e.getMessage),
          b => log.info(s"Current source asset balance: ${b.toString}")
        )

      quantity = Try(
        ((sourceAssetBalance.head.availableBalance / currentPrice) * defaultLeverage / 2) // Don't jump in with both feet
          .setScale(3, RoundingMode.HALF_UP)
      ).getOrElse(BigDecimal(0))

      marketOrder <-
        if (quantity > 0)
          newMarketOrder(NewMarketOrderRequest(symbol.toString, OrderSide.BUY, quantity = quantity))
            .tapBoth(
              e => log.error(e.getMessage),
              response =>
                log.info(response.toStringWithFields) *> TelegramClient
                  .broadcastMessage(Set(ChatId(defaultChatId)), response.toStringWithFields)
                  .tapError(t => log.error(t.getMessage))
                  .ignore
            )
        else ZIO.fail(GenericHttpError(s"Balance is insufficient ${sourceAssetBalance.map(_.toStringWithFields)}"))

      _ <- queryOpenPositions(symbol)
    } yield marketOrder

  protected def closeExistingPosition(symbol: Symbols): ZIO[
    BinanceFutureClient with BinanceFutureClientEnv with TelegramClient,
    HttpClientError,
    Responses.OrderPlacedResponse
  ] = {
    for {
      positionsInfo <- queryOpenPositions(symbol)
      marketOrderResponse <-
        if (positionsInfo.nonEmpty) {
          val positionInfo = positionsInfo.head
          //The negative amount represents a Short position, whereas a positive one represents a Long position
          //To close a Long position open a sell position
          //To close a Short position open a buy position
          newMarketOrder(
            NewMarketOrderRequest(
              symbol.toString,
              if (positionInfo.positionAmt > 0) OrderSide.SELL else OrderSide.BUY,
              quantity = positionInfo.positionAmt.abs.setScale(3, RoundingMode.HALF_UP)
            )
          ).tapBoth(
            e => log.error(e.getMessage),
            response =>
              log.info(response.toStringWithFields) *> TelegramClient
                .broadcastMessage(Set(ChatId(defaultChatId)), response.toStringWithFields)
                .tapError(t => log.error(t.getMessage))
                .ignore
          )
        } else
          ZIO.fail(GenericHttpError("There isn't any open position to close"))
    } yield marketOrderResponse
  }

}
