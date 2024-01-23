package io.odin.binance.stream

import scala.util.Random

object model {
  trait WSRequest {
    def method: String
    def id: Int
    def params: List[Any]
  }

  case class Subscribe(params: List[String], method: String = "SUBSCRIBE", id: Int = Math.abs(Random.nextInt(1000000)))
      extends WSRequest

  trait WSResponse

  case class Subscribed(result: String, id: Int) extends WSResponse

  case class ErrorResponse(msg: String) extends WSResponse
}
