package io.odin.ta4s.domain

trait TradingParameters {
  val defaultLeverage = 6

  // 2: success rate 60
  // 2> <4: success rate 72
  // 4: success rate 80
  // 4.5: success rate 100
  val defaultStopLoss = BigDecimal(5) * 0.01

  val takeProfit = BigDecimal(1) * 0.01
}
