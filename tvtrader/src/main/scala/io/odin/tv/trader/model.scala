package io.odin.tv.trader

import io.odin.binance.client.model.Enums.{OrderSide, PositionSide}

case class ActionRequest(orderSide: OrderSide, position: PositionSide)
