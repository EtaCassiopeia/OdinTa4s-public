package io.odin

import cats.implicits._

package object binance {
  object OptionEx {
    def when[T](condition: => Boolean)(value: => T): Option[T] =
      condition.guard[Option].map(_ => value)
  }
}
