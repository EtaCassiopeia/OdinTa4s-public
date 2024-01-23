package io.odin.trader

import io.odin.binance.client.BinanceFutureClient._
import io.odin.binance.client.model.Enums.OrderSide
import io.odin.binance.client.model.Requests.{NewMarketOrderRequest, RequestBySymbol}
import io.odin.binance.client.model.{Responses, Symbols}
import io.odin.binance.http.{GenericHttpError, HttpClientError}
import io.odin.common.PrettyCaseClass._
import io.odin.ta4s.domain.{Position, TradingParameters}
import io.odin.ta4s.strategy.OpenPosition
import io.odin.telegram.TelegramClient
import io.odin.telegram.TelegramClient.TelegramClient
import io.odin.telegram.domain.ChatId
import zio._
import zio.logging.log

import scala.math.BigDecimal.RoundingMode
import scala.util.Try

trait BinanceSupport extends TradingParameters {

  protected val symbol: Symbols

  protected val defaultChatId: Long

  protected val strategyName: String

  protected def queryOpenPositions
    : ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, List[Responses.OpenPosition]] =
    positionInfo(RequestBySymbol(symbol.toString))
      .map(_.filter(r => r.positionAmt != 0))
      .tapBoth(
        e => log.error(e.getMessage),
        positions => log.info(s"Open positions: ${positions.map(_.toStringWithFields).mkString(",")}")
      )

  protected def openNewPosition(
    openPositionRequest: OpenPosition
  ): ZIO[
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
        ((sourceAssetBalance.head.availableBalance / openPositionRequest.price) * defaultLeverage / 2) // Don't jump in with both feet
          .setScale(3, RoundingMode.HALF_UP)
      ).getOrElse(BigDecimal(0))

      /*stopPrice = (openPositionRequest.position match {
          case Position.Long  => openPositionRequest.price * (1 - defaultStopLoss)
          case Position.Short => openPositionRequest.price * (1 + defaultStopLoss)
        }).setScale(2, RoundingMode.HALF_UP)*/

      marketOrder <-
        if (quantity > 0)
          newMarketOrder(
            NewMarketOrderRequest(
              symbol.toString,
              if (openPositionRequest.position == Position.Long) OrderSide.BUY else OrderSide.SELL,
              quantity = quantity
            )
          )
            .tapBoth(
              e => log.error(e.getMessage),
              response =>
                log.info(response.toStringWithFields) *> TelegramClient
                  .broadcastMessage(Set(ChatId(defaultChatId)), s"$strategyName: ${response.toStringWithFields}")
                  .tapError(t => log.error(t.getMessage))
                  .ignore
            )
        else ZIO.fail(GenericHttpError(s"Balance is insufficient ${sourceAssetBalance.map(_.toStringWithFields)}"))

      _ <- queryOpenPositions
    } yield marketOrder

  protected def closeExistingPosition(): ZIO[
    BinanceFutureClient with BinanceFutureClientEnv with TelegramClient,
    HttpClientError,
    Responses.OrderPlacedResponse
  ] = {
    for {
      positionsInfo <- queryOpenPositions
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
                .broadcastMessage(Set(ChatId(defaultChatId)), s"$strategyName: ${response.toStringWithFields}")
                .tapError(t => log.error(t.getMessage))
                .ignore
          )
        } else
          ZIO.fail(GenericHttpError("There isn't any open position to close"))
    } yield marketOrderResponse
  }
}
