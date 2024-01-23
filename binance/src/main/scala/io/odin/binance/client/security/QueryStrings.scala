package io.odin.binance.client.security

import io.lemonlabs.uri.QueryString
import io.odin.binance.http.QueryStringConverter
import io.odin.common.Utils
import sttp.model.QueryParams

import scala.language.implicitConversions

object QueryStrings {

  private def queryStringToMultiSeq(queryString: String): Seq[(String, Vector[String])] =
    queryString
      .split("&")
      .toList
      .map { param =>
        val pair = param.split("=")
        (pair.head, Vector(pair.last))
      }

  implicit def queryStringToQueryParams(queryString: String): QueryParams =
    QueryParams.fromMultiSeq(
      queryStringToMultiSeq(queryString).toList
    )

  def queryParams[T: QueryStringConverter](request: T): QueryParams = {
    QueryStringConverter[T].to(request)
  }

  def signedQueryParams[T: QueryStringConverter](request: T, apiSecret: String): QueryParams = {
    val requestAsQuery =
      QueryString
        .parse(QueryStringConverter[T].to(request))
        .toString() + s"&recvWindow=10000&timestamp=${Utils.currentTimestampUTC}"

    //Recreate the query string from QueryParameter to make sure that it preserves the order
    val queryStringSeq = queryStringToMultiSeq(requestAsQuery)
    val queryParams = QueryParams.fromMultiSeq(queryStringSeq)
    val queryString = queryParams.toSeq.map { case (k, v) => s"$k=$v" }.mkString("&")

    val signature = HMAC.sha256(apiSecret, queryString)
    QueryParams.fromMultiSeq(queryStringSeq :+ ("signature" -> Vector(signature)))
  }

  def signedEmptyQueryParams(apiSecret: String): QueryParams = {
    val requestAsQuery = s"recvWindow=10000&timestamp=${Utils.currentTimestampUTC}"
    val signature = HMAC.sha256(apiSecret, requestAsQuery)
    QueryParams.fromMultiSeq(queryStringToMultiSeq(requestAsQuery) :+ ("signature" -> Vector(signature)))
  }
}
