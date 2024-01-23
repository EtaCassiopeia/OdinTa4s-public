package io.odin.binance.client.model

import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.odin.binance.http.QueryStringConverter

object Enums {

  sealed trait OrderSide extends EnumEntry

  object OrderSide extends Enum[OrderSide] with CirceEnum[OrderSide] {
    val values = findValues

    case object SELL extends OrderSide

    case object BUY extends OrderSide

    case object UNKNOWN extends OrderSide

    implicit val orderSideQueryStringConverter: QueryStringConverter[OrderSide] =
      QueryStringConverter.enumEntryConverter[OrderSide]()
  }

  sealed trait PositionSide extends EnumEntry

  object PositionSide extends Enum[PositionSide] with CirceEnum[PositionSide] {
    val values = findValues

    case object BOTH extends PositionSide

    case object LONG extends PositionSide

    case object SHORT extends PositionSide

    implicit val orderPositionSideQueryStringConverter: QueryStringConverter[PositionSide] =
      QueryStringConverter.enumEntryConverter[PositionSide]()
  }

  sealed trait OrderType extends EnumEntry

  object OrderType extends Enum[OrderType] with CirceEnum[OrderType] {
    val values = findValues

    case object LIMIT extends OrderType

    case object MARKET extends OrderType

    case object STOP extends OrderType

    case object STOP_MARKET extends OrderType

    case object TAKE_PROFIT extends OrderType

    case object TAKE_PROFIT_MARKET extends OrderType

    case object TRAILING_STOP_MARKET extends OrderType

    implicit val orderTypeQueryStringConverter: QueryStringConverter[OrderType] =
      QueryStringConverter.enumEntryConverter[OrderType]()

  }

  sealed trait TimeInForce extends EnumEntry

  object TimeInForce extends Enum[TimeInForce] with CirceEnum[TimeInForce] {
    val values = findValues

    case object GTC extends TimeInForce // Good-Til-Canceled
    case object IOT extends TimeInForce // Immediate or Cancel
    case object FOK extends TimeInForce // Fill or Kill

    implicit val timeInForceQueryStringConverter: QueryStringConverter[TimeInForce] =
      QueryStringConverter.enumEntryConverter[TimeInForce]()
  }

  sealed trait WorkingType extends EnumEntry

  object WorkingType extends Enum[WorkingType] with CirceEnum[WorkingType] {
    val values = findValues

    case object MARK_PRICE extends WorkingType

    case object CONTRACT_PRICE extends WorkingType

    implicit val workingTypeQueryStringConverter: QueryStringConverter[WorkingType] =
      QueryStringConverter.enumEntryConverter[WorkingType]()
  }

  sealed trait OrderCreateResponseType extends EnumEntry

  object OrderCreateResponseType extends Enum[OrderCreateResponseType] with CirceEnum[OrderCreateResponseType] {
    val values = findValues

    case object ACK extends OrderCreateResponseType

    case object RESULT extends OrderCreateResponseType

    case object FULL extends OrderCreateResponseType

    implicit val orderResponseTypeQueryStringConverter: QueryStringConverter[OrderCreateResponseType] =
      QueryStringConverter.enumEntryConverter[OrderCreateResponseType]()

  }

  sealed trait OrderStatus extends EnumEntry

  object OrderStatus extends Enum[OrderStatus] with CirceEnum[OrderStatus] {
    val values = findValues

    case object NEW extends OrderStatus

    case object PARTIALLY_FILLED extends OrderStatus

    case object FILLED extends OrderStatus

    case object CANCELED extends OrderStatus

    case object PENDING_CANCEL extends OrderStatus

    case object REJECTED extends OrderStatus

    case object EXPIRED extends OrderStatus

    case object MARKET extends OrderStatus

    implicit val orderStatusQueryStringConverter: QueryStringConverter[OrderStatus] =
      QueryStringConverter.enumEntryConverter[OrderStatus]()

  }

}

object Requests {

  import Enums._

  case class NewStopLimitOrderRequest(
    symbol: String,
    side: OrderSide,
    quantity: BigDecimal,
    price: BigDecimal,
    stopPrice: BigDecimal,
    `type`: OrderType = OrderType.STOP,
    timeInForce: TimeInForce = TimeInForce.GTC,
    priceProtect: String = "TRUE",
    newOrderRespType: OrderCreateResponseType = OrderCreateResponseType.RESULT,
    positionSide: Option[PositionSide] = None,
    reduceOnly: Option[String] = None,
    newClientOrderId: Option[String] = None,
    closePosition: Option[String],
    activationPrice: Option[BigDecimal] = None,
    callbackRate: Option[BigDecimal] = None,
    workingType: Option[WorkingType] = None
  )

  case class NewMarketOrderRequest(
    symbol: String,
    side: OrderSide,
    quantity: BigDecimal,
    `type`: OrderType = OrderType.MARKET,
    newOrderRespType: OrderCreateResponseType = OrderCreateResponseType.RESULT,
    newClientOrderId: Option[String] = None,
    closePosition: Option[String] = None
  )

  case class NewStopMarketOrderRequest(
    symbol: String,
    side: OrderSide,
    quantity: BigDecimal,
    stopPrice: BigDecimal,
    `type`: OrderType = OrderType.STOP_MARKET,
    newOrderRespType: OrderCreateResponseType = OrderCreateResponseType.RESULT,
    newClientOrderId: Option[String] = None,
    closePosition: Option[String] = None,
    priceProtect: String = "TRUE"
  )

  case class NewTakeProfitMarketOrderRequest(
    symbol: String,
    side: OrderSide,
    quantity: BigDecimal,
    stopPrice: BigDecimal,
    `type`: OrderType = OrderType.TAKE_PROFIT_MARKET,
    newOrderRespType: OrderCreateResponseType = OrderCreateResponseType.RESULT,
    newClientOrderId: Option[String] = None,
    closePosition: Option[String] = None,
    priceProtect: String = "TRUE"
  )

  case class QueryOrderRequest(symbol: String, orderId: Option[Long] = None, origClientOrderId: Option[String] = None)

  case class RequestBySymbol(symbol: String)

  case class ChangeLeverageRequest(symbol: String, leverage: Int)

  case class ChangeLeverageResponse(symbol: String, leverage: Int, maxNotionalValue: BigDecimal)
}

object Responses {

  import Enums._

  case class OrderPlacedResponse(
    clientOrderId: String,
    cumQty: BigDecimal,
    cumQuote: BigDecimal,
    executedQty: BigDecimal,
    orderId: Long,
    avgPrice: BigDecimal,
    origQty: BigDecimal,
    price: BigDecimal,
    reduceOnly: Boolean,
    side: OrderSide,
    positionSide: PositionSide,
    status: OrderStatus,
    stopPrice: BigDecimal, // please ignore when order type is TRAILING_STOP_MARKET
    closePosition: Boolean, // if Close-All
    symbol: String,
    timeInForce: TimeInForce,
    `type`: OrderType,
    origType: OrderType,
    activatePrice: Option[BigDecimal], // activation price, only return with TRAILING_STOP_MARKET order
    priceRate: Option[BigDecimal], // callback rate, only return with TRAILING_STOP_MARKET order
    updateTime: Long,
    workingType: WorkingType,
    priceProtect: Option[Boolean] // if conditional order trigger is protected
  )

  case class QueryOrderResponse(
    avgPrice: String,
    clientOrderId: String,
    cumQuote: String,
    executedQty: String,
    orderId: Long,
    origQty: String,
    origType: OrderType,
    price: String,
    reduceOnly: Boolean,
    side: OrderSide,
    positionSide: PositionSide,
    status: OrderStatus,
    stopPrice: String, // please ignore when order type is TRAILING_STOP_MARKET
    closePosition: Boolean, // if Close-All
    symbol: String,
    time: Long, // order time
    timeInForce: TimeInForce,
    `type`: OrderType,
    activatePrice: Option[String], // activation price, only return with TRAILING_STOP_MARKET order
    priceRate: Option[String], // callback rate, only return with TRAILING_STOP_MARKET order
    updateTime: Long, // update time
    workingType: WorkingType,
    priceProtect: Option[Boolean] // if conditional order trigger is protected
  )

  case class Response(code: Int, msg: String)

  case class FutureAccountResponse(
    accountAlias: String, // unique account code
    asset: String, // asset name
    balance: BigDecimal, // wallet balance
    crossWalletBalance: BigDecimal, // crossed wallet balance
    crossUnPnl: BigDecimal, // unrealized profit of crossed positions
    availableBalance: BigDecimal, // available balance
    maxWithdrawAmount: BigDecimal
  )

  case class Position(
    symbol: String, // symbol name
    initialMargin: BigDecimal, // initial margin required with current mark price
    maintMargin: BigDecimal, // maintenance margin required
    unrealizedProfit: BigDecimal, // unrealized profit
    positionInitialMargin: BigDecimal, // initial margin required for positions with current mark price
    openOrderInitialMargin: BigDecimal, // initial margin required for open orders with current mark price
    leverage: BigDecimal, // current initial leverage
    isolated: Boolean, // if the position is isolated
    entryPrice: BigDecimal, // average entry price
    maxNotional: BigDecimal, // maximum available notional with current leverage
    positionSide: PositionSide, // position side
    positionAmt: BigDecimal // position amount
  )

  case class Asset(
    asset: String, // asset name
    walletBalance: BigDecimal, // wallet balance
    unrealizedProfit: BigDecimal, // unrealized profit
    marginBalance: BigDecimal, // margin balance
    maintMargin: BigDecimal, // maintenance margin required
    initialMargin: BigDecimal, // total initial margin required with current mark price
    positionInitialMargin: BigDecimal, //initial margin required for positions with current mark price
    openOrderInitialMargin: BigDecimal, // initial margin required for open orders with current mark price
    crossWalletBalance: BigDecimal, // crossed wallet balance
    crossUnPnl: BigDecimal, // unrealized profit of crossed positions
    availableBalance: BigDecimal, // available balance
    maxWithdrawAmount: BigDecimal // maximum amount for transfer out
  )

  case class AccountInfoResponse(
    feeTier: Long, // account commisssion tier
    canTrade: Boolean, // if can trade
    canDeposit: Boolean, // if can transfer in asset
    canWithdraw: Boolean, // if can transfer out asset
    updateTime: Long,
    totalInitialMargin: BigDecimal, // total initial margin required with current mark price (useless with isolated positions)
    totalMaintMargin: BigDecimal, // total maintenance margin required
    totalWalletBalance: BigDecimal, // total wallet balance
    totalUnrealizedProfit: BigDecimal, // total unrealized profit
    totalMarginBalance: BigDecimal, // total margin balance
    totalPositionInitialMargin: BigDecimal, // initial margin required for positions with current mark price
    totalOpenOrderInitialMargin: BigDecimal, // initial margin required for open orders with current mark price
    totalCrossWalletBalance: BigDecimal, // crossed wallet balance
    totalCrossUnPnl: BigDecimal, // unrealized profit of crossed positions
    availableBalance: BigDecimal, // available balance
    maxWithdrawAmount: BigDecimal, // maximum amount for transfer out
    assets: List[Asset],
    // only "BOTH" positions will be returned with One-way mode
    // only "LONG" and "SHORT" positions will be returned with Hedge mode
    positions: List[Position] // positions of all sumbols in the market are returned
  )

  case class OpenPosition(
    entryPrice: BigDecimal,
    marginType: String, //cross, isolated TODO: Create an Enum
    isAutoAddMargin: String,
    isolatedMargin: BigDecimal,
    leverage: BigDecimal,
    liquidationPrice: BigDecimal,
    markPrice: BigDecimal,
    maxNotionalValue: BigDecimal,
    positionAmt: BigDecimal,
    symbol: String,
    unRealizedProfit: BigDecimal,
    positionSide: PositionSide
  )

}
