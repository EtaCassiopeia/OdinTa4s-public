package io.odin.binance.client

import io.circe.generic.auto._
import io.odin.binance.client.model.Requests.{
  ChangeLeverageRequest,
  ChangeLeverageResponse,
  NewMarketOrderRequest,
  NewStopLimitOrderRequest,
  NewStopMarketOrderRequest,
  QueryOrderRequest,
  RequestBySymbol
}
import io.odin.binance.client.model.Responses._
import io.odin.binance.http.HttpClient._
import io.odin.binance.http.{Credentials, HttpClientError}
import sttp.model.Uri
import zio.{Has, ULayer, ZIO, ZLayer}

object BinanceFutureClient {

  type BinanceFutureClient = Has[BinanceFutureClient.Service]
  type BinanceFutureClientBaseEnv = HttpClient with HttpClientEnv
  type BinanceFutureClientEnv = BinanceFutureClientBaseEnv with Has[Credentials]

  private val baseUrl = Uri("https", "fapi.binance.com")

  trait Service {
    def newMarketOrder(
      newMarketOrderRequest: NewMarketOrderRequest
    ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, OrderPlacedResponse]

    def newStopMarketOrder(
      newStopMarketOrderRequest: NewStopMarketOrderRequest
    ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, OrderPlacedResponse]

    def newStopLimitOrder(
      newStopLimitOrderRequest: NewStopLimitOrderRequest
    ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, OrderPlacedResponse]

    def queryOrder(
      queryOrderRequest: QueryOrderRequest
    ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, QueryOrderResponse]

    def cancelAllOpenOrder(
      cancelAllOpenOrdersRequest: RequestBySymbol
    ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, Response]

    def currentAllOpenOrders(
      currentAllOpenOrders: RequestBySymbol
    ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, List[QueryOrderResponse]]

    def futuresAccountBalance()
      : ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, List[FutureAccountResponse]]

    def futuresAccountInfo()
      : ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, AccountInfoResponse]

    def positionInfo(
      requestBySymbol: RequestBySymbol
    ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, List[OpenPosition]]

    def changeLeverage(
      changeLeverageRequest: ChangeLeverageRequest
    ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, ChangeLeverageResponse]
  }

  object Service {
    val live: Service = new Service {
      override def newMarketOrder(
        newMarketOrderRequest: NewMarketOrderRequest
      ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, OrderPlacedResponse] =
        signedPost[NewMarketOrderRequest, OrderPlacedResponse](baseUrl.path("/fapi/v1/order"), newMarketOrderRequest)

      override def newStopMarketOrder(
        newStopMarketOrderRequest: NewStopMarketOrderRequest
      ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, OrderPlacedResponse] =
        signedPost[NewStopMarketOrderRequest, OrderPlacedResponse](
          baseUrl.path("/fapi/v1/order"),
          newStopMarketOrderRequest
        )

      override def newStopLimitOrder(
        newStopLimitOrderRequest: NewStopLimitOrderRequest
      ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, OrderPlacedResponse] =
        signedPost[NewStopLimitOrderRequest, OrderPlacedResponse](
          baseUrl.path("/fapi/v1/order"),
          newStopLimitOrderRequest
        )

      override def queryOrder(
        queryOrderRequest: QueryOrderRequest
      ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, QueryOrderResponse] =
        signedGet[QueryOrderRequest, QueryOrderResponse](baseUrl.path("/fapi/v2/order"), queryOrderRequest)

      override def cancelAllOpenOrder(
        cancelAllOpenOrdersRequest: RequestBySymbol
      ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, Response] =
        signedDelete[RequestBySymbol, Response](baseUrl.path("/fapi/v2/allOpenOrders"), cancelAllOpenOrdersRequest)

      override def currentAllOpenOrders(
        currentAllOpenOrders: RequestBySymbol
      ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, List[QueryOrderResponse]] =
        signedGet[RequestBySymbol, List[QueryOrderResponse]](baseUrl.path("/fapi/v2/openOrders"), currentAllOpenOrders)

      override def futuresAccountBalance()
        : ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, List[FutureAccountResponse]] =
        signedGet[List[FutureAccountResponse]](baseUrl.path("/fapi/v2/balance"))

      override def futuresAccountInfo()
        : ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, AccountInfoResponse] =
        signedGet[AccountInfoResponse](baseUrl.path("/fapi/v2/account"))

      override def positionInfo(
        requestBySymbol: RequestBySymbol
      ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, List[OpenPosition]] =
        signedGet[RequestBySymbol, List[OpenPosition]](baseUrl.path("/fapi/v2/positionRisk"), requestBySymbol)

      override def changeLeverage(
        changeLeverageRequest: ChangeLeverageRequest
      ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, ChangeLeverageResponse] =
        signedPost[ChangeLeverageRequest, ChangeLeverageResponse](
          baseUrl.path("/fapi/v1/leverage"),
          changeLeverageRequest
        )
    }
  }

  def live: ULayer[Has[Service]] =
    ZLayer.succeed(Service.live)

  def newMarketOrder(
    newMarketOrderRequest: NewMarketOrderRequest
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, OrderPlacedResponse] =
    ZIO.accessM(_.get.newMarketOrder(newMarketOrderRequest))

  def newStopMarketOrder(
    newStopMarketOrderRequest: NewStopMarketOrderRequest
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, OrderPlacedResponse] =
    ZIO.accessM(_.get.newStopMarketOrder(newStopMarketOrderRequest))

  def newStopLimitOrder(
    newStopLimitOrderRequest: NewStopLimitOrderRequest
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, OrderPlacedResponse] =
    ZIO.accessM(_.get.newStopLimitOrder(newStopLimitOrderRequest))

  def queryOrder(
    queryOrderRequest: QueryOrderRequest
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, QueryOrderResponse] =
    ZIO.accessM(_.get.queryOrder(queryOrderRequest))

  def cancelAllOpenOrder(
    cancelAllOpenOrdersRequest: RequestBySymbol
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, Response] =
    ZIO.accessM(_.get.cancelAllOpenOrder(cancelAllOpenOrdersRequest))

  def currentAllOpenOrders(
    currentAllOpenOrders: RequestBySymbol
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, List[QueryOrderResponse]] =
    ZIO.accessM(_.get.currentAllOpenOrders(currentAllOpenOrders))

  def futuresAccountBalance()
    : ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, List[FutureAccountResponse]] =
    ZIO.accessM(_.get.futuresAccountBalance())

  def futuresAccountInfo(): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, AccountInfoResponse] =
    ZIO.accessM(_.get.futuresAccountInfo())

  def positionInfo(
    requestBySymbol: RequestBySymbol
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, List[OpenPosition]] =
    ZIO.accessM(_.get.positionInfo(requestBySymbol))

  def changeLeverage(
    changeLeverageRequest: ChangeLeverageRequest
  ): ZIO[BinanceFutureClient with BinanceFutureClientEnv, HttpClientError, ChangeLeverageResponse] =
    ZIO.accessM(_.get.changeLeverage(changeLeverageRequest))
}
