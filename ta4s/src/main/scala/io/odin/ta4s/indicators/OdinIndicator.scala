package io.odin.ta4s.indicators

trait OdinIndicator[T] {
  def addElement(element: T): Unit
  def +(element: T): Unit
  def valueAt(index: Int): Either[IndicatorError, Double]
  def value: Either[IndicatorError, Double]
  def lastIndex: Int
}
