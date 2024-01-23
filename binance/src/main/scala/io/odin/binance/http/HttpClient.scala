package io.odin.binance.http

import io.circe
import io.circe.Decoder
import io.odin.binance.client.model.Symbols
import io.odin.binance.client.security.QueryStrings._
import sttp.client
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.circe.asJson
import sttp.client.{ResponseError, basicRequest}
import sttp.model.{HeaderNames, QueryParams, StatusCode, Uri}
import zio.clock.Clock
import zio.duration.durationInt
import zio.logging.{LogLevel, Logging, log}
import zio.{Has, Schedule, ULayer, ZIO, ZLayer}

case class Credentials(apiKey: String, secretKey: String)

case class SubAccount(symbol: Symbols, credentialsLayer: ULayer[Has[Credentials]])

object HttpClient {

  type HttpClient = Has[Service]
  type HttpClientEnv = SttpClient with Clock with Logging

  trait Service {
    def get[Response: Decoder](
      uri: Uri,
      queryParams: QueryParams
    ): ZIO[HttpClientEnv, HttpClientError, Response]

    def signedGet[Response: Decoder](
      uri: Uri,
      queryParams: QueryParams
    ): ZIO[HttpClientEnv with Has[Credentials], HttpClientError, Response]

    def signedPost[Response: Decoder](
      uri: Uri,
      queryParams: QueryParams
    ): ZIO[HttpClientEnv with Has[Credentials], HttpClientError, Response]

    def signedDelete[Response: Decoder](
      uri: Uri,
      queryParams: QueryParams
    ): ZIO[HttpClientEnv with Has[Credentials], HttpClientError, Response]
  }

  object Service {
    val live: Service = new Service {

      private val scheduler = Schedule.exponential(50.millis) && (Schedule.recurs(5)
        && Schedule.recurWhile[HttpClientError] {
          case TooManyRequests(_) => false
          case _                  => true
        })

      private def enrichedRequest(extraParams: Map[String, String] = Map.empty) =
        basicRequest
          .headers(
            Map(
              HeaderNames.ContentType -> "application/json; charset=utf-8",
              HeaderNames.Accept -> "application/json; charset=utf-8"
            ) ++ extraParams
          )

      private def sendRequest[Response](
        request: client.Request[Either[ResponseError[circe.Error], Response], Nothing]
      ): ZIO[HttpClientEnv, HttpClientError, Response] =
        log(LogLevel.Info)(s"Sending request: " + request.toCurl) *>
          SttpClient
            .send(request)
            .timeoutFail(RequestTimedOut(s"Request timeout: $request"))(30.seconds)
            .reject {
              case r if r.code == StatusCode.TooManyRequests => TooManyRequests(r.toString())
            }
            .map(_.body)
            .absolve
            .bimap(err => GenericHttpError(err.getMessage), identity)
            .retry(scheduler)

      override def get[Response: Decoder](
        uri: Uri,
        queryParams: QueryParams
      ): ZIO[HttpClientEnv, HttpClientError, Response] = {

        val getRequest = enrichedRequest()
          .response(asJson[Response])
          .get(uri.params(queryParams))

        sendRequest(getRequest)
      }

      override def signedGet[Response: Decoder](
        uri: Uri,
        queryParams: QueryParams
      ): ZIO[HttpClientEnv with Has[Credentials], HttpClientError, Response] =
        for {
          apiKey <- ZIO.access[Has[Credentials]](_.get.apiKey)
          postRequest = enrichedRequest(Map("X-MBX-APIKEY" -> apiKey))
            .response(asJson[Response])
            .get(uri.params(queryParams))
          response <- sendRequest(postRequest)
        } yield response

      override def signedPost[Response: Decoder](
        uri: Uri,
        queryParams: QueryParams
      ): ZIO[HttpClientEnv with Has[Credentials], HttpClientError, Response] =
        for {
          apiKey <- ZIO.access[Has[Credentials]](_.get.apiKey)
          postRequest = enrichedRequest(Map("X-MBX-APIKEY" -> apiKey))
            .response(asJson[Response])
            .post(uri.params(queryParams))
          response <- sendRequest(postRequest)
        } yield response

      override def signedDelete[Response: Decoder](
        uri: Uri,
        queryParams: QueryParams
      ): ZIO[HttpClientEnv with Has[Credentials], HttpClientError, Response] =
        for {
          apiKey <- ZIO.access[Has[Credentials]](_.get.apiKey)
          postRequest = enrichedRequest(Map("X-MBX-APIKEY" -> apiKey))
            .response(asJson[Response])
            .delete(uri.params(queryParams))
          response <- sendRequest(postRequest)
        } yield response
    }
  }

  def live: ULayer[Has[Service]] =
    ZLayer.succeed(Service.live)

  def get[Request: QueryStringConverter, Response: Decoder](
    uri: Uri,
    request: Request
  ): ZIO[HttpClient with HttpClientEnv, HttpClientError, Response] =
    ZIO.accessM(_.get.get(uri, queryParams(request)))

  def signedGet[Request: QueryStringConverter, Response: Decoder](
    uri: Uri,
    request: Request
  ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, Response] =
    for {
      secretKey <- ZIO.access[Has[Credentials]](_.get.secretKey)
      response <- ZIO.accessM[HttpClient with HttpClientEnv with Has[Credentials]](
        _.get.signedGet(uri, signedQueryParams(request, secretKey))
      )
    } yield response

  def signedGet[Response: Decoder](
    uri: Uri
  ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, Response] =
    for {
      secretKey <- ZIO.access[Has[Credentials]](_.get.secretKey)
      response <- ZIO.accessM[HttpClient with HttpClientEnv with Has[Credentials]](
        _.get.signedGet(uri, signedEmptyQueryParams(secretKey))
      )
    } yield response

  def signedPost[Request: QueryStringConverter, Response: Decoder](
    uri: Uri,
    request: Request
  ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, Response] =
    for {
      secretKey <- ZIO.access[Has[Credentials]](_.get.secretKey)
      response <- ZIO.accessM[HttpClient with HttpClientEnv with Has[Credentials]](
        _.get.signedPost(uri, signedQueryParams(request, secretKey))
      )
    } yield response

  def signedDelete[Request: QueryStringConverter, Response: Decoder](
    uri: Uri,
    request: Request
  ): ZIO[HttpClient with HttpClientEnv with Has[Credentials], HttpClientError, Response] =
    for {
      secretKey <- ZIO.access[Has[Credentials]](_.get.secretKey)
      response <- ZIO.accessM[HttpClient with HttpClientEnv with Has[Credentials]](
        _.get.signedDelete(uri, signedQueryParams(request, secretKey))
      )
    } yield response

}
