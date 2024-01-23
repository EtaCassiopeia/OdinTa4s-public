package io.odin.crypto.ml

import io.circe
import org.scalatest._
import matchers.should._
import org.scalatest.funspec.AnyFunSpec
import io.circe.parser.decode
import io.odin.crypto.ml.Payload.OrderSide

class PayloadSpec extends AnyFunSpec with Matchers with EitherValues {
  describe("Payload") {
    it("should be able to decode MarketIndex") {

      val encodedValue: Either[circe.Error, Payload] =
        decode[Payload]("""{"bullBear":"BULL 13.56","_7dayTrend":"Bull market weakening."}""")
      val marketIndex = MarketIndex("BULL 13.56", "Bull market weakening.")

      assert(encodedValue.contains(marketIndex))
    }

    it("should be able to decode sell TradeAlert") {
      val encodedValue: Either[circe.Error, Payload] =
        decode[Payload](
          """{"side":"sell", "productCode":"BTC-USD", "price":"9543.01", "time":"2020-02-24T18:38:01.6484377Z", "bot":"ML"}"""
        )
      val tradeAlert = TradeAlert(
        OrderSide.sell,
        "BTC-USD",
        price = BigDecimal(9543.01),
        time = "2020-02-24T18:38:01.6484377Z",
        bot = "ML"
      )

      assert(encodedValue.contains(tradeAlert))
    }

    it("should be able to decode buy TradeAlert") {
      val encodedValue: Either[circe.Error, Payload] =
        decode[Payload](
          """{"side":"buy", "productCode":"ETH-USD", "price":"264.56", "time":"2020-02-22T01:22:42.2901814Z", "trailingStop":"-0.0397119140625", "buyTrigger":"3.07325747536978", "tslAdjust":"-0.00591409301757813", "nnValue":"5.20176589036058", "bot":"ML"}""".stripMargin
        )
      val tradeAlert = TradeAlert(
        OrderSide.buy,
        "ETH-USD",
        price = BigDecimal(264.56),
        time = "2020-02-22T01:22:42.2901814Z",
        bot = "ML",
        trailingStop = Some(BigDecimal(-0.0397119140625)),
        buyTrigger = Some(BigDecimal(3.07325747536978)),
        tslAdjust = Some(BigDecimal(-0.00591409301757813)),
        nnValue = Some(BigDecimal(5.20176589036058))
      )

      assert(encodedValue.contains(tradeAlert))
    }

    it("should be able to decode PricePrediction") {
      val encodedValue: Either[circe.Error, Payload] =
        decode[Payload](
          """{"crypto":"USD-ETH", "prediction_6hr":"0.10868181010881219", "prediction_12hr":"-2.6982040678439647", "manip_value":"1"}"""
        )
      val pricePrediction = PricePrediction(
        crypto = "USD-ETH",
        prediction_6hr = BigDecimal(0.10868181010881219),
        prediction_12hr = BigDecimal(-2.6982040678439647),
        manip_value = 1
      )

      assert(encodedValue.contains(pricePrediction))
    }

  }
}
