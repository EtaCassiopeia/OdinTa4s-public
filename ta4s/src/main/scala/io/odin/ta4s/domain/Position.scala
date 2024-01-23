package io.odin.ta4s.domain

import enumeratum.{Enum, EnumEntry}

sealed trait Position extends EnumEntry

object Position extends Enum[Position] {
  val values = findValues

  case object Long extends Position

  case object Short extends Position
}

sealed trait Signal extends EnumEntry

object Signal extends Enum[Signal] {
  val values = findValues

  case object LongSignal extends Signal

  case object ShortSignal extends Signal

  case object Unknown extends Signal
}
