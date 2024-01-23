package io.odin.binance.client.model

import enumeratum.EnumEntry
import enumeratum.Enum

sealed trait Symbols extends EnumEntry {
  def sourceAsset: Asset
  def targetAsset: Asset
}

object Symbols extends Enum[Symbols] {
  val values = findValues

  case object BTCUSDT extends Symbols {
    override def sourceAsset: Asset = Assets.USDT

    override def targetAsset: Asset = Assets.BTC
  }

  case object ETHUSDT extends Symbols {
    override def sourceAsset: Asset = Assets.USDT

    override def targetAsset: Asset = Assets.ETH
  }

}

sealed trait Asset extends EnumEntry

object Assets extends Enum[Asset] {
  val values = findValues

  case object USDT extends Asset
  case object EUR extends Asset
  case object BTC extends Asset
  case object ETH extends Asset
}
