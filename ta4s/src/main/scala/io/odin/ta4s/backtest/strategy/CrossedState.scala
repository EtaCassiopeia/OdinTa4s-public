package io.odin.ta4s.backtest.strategy

import enumeratum.{Enum, EnumEntry}

sealed trait CrossedState extends EnumEntry

object CrossedState extends Enum[CrossedState] {
  val values = findValues

  case object CrossedDown extends CrossedState

  case object CrossedUp extends CrossedState

}
