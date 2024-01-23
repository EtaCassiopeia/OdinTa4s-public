package io.odin.ta4s.indicators

sealed trait IndicatorError

case object NotInitializedError extends IndicatorError
case object InsufficientDateError extends IndicatorError
case object OutOfIndexError extends IndicatorError
