package io.odin.binance.stream

import io.odin.binance.client.model.CandlestickRequest.Interval
import model.{Subscribe, WSResponse}

trait SubscribeRequest[T <: WSResponse] {
  def request(symbol: String, interval: Interval): Subscribe
}

object SubscribeRequest {
  def apply[T <: WSResponse](implicit subscribeRequest: SubscribeRequest[T]): SubscribeRequest[T] = subscribeRequest
}
