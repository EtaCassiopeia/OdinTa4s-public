package io.odin.ta4s.strategy.harmonic.patterns

import org.ta4j.core.num.Num

class ZigZagPatterns()(implicit numFunction: Double => Num) {
  import ZigZagPatterns._
  def isBat(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.382 && xab <= 0.5
    val _abc = abc >= 0.382 && abc <= 0.886
    val _bcd = bcd >= 1.618 && bcd <= 2.618
    val _xad = xad <= 0.618 && xad <= 1.000 // 0.886
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isAntiBat(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.500 && xab <= 0.886 // 0.618
    val _abc = abc >= 1.000 && abc <= 2.618 // 1.13 --> 2.618
    val _bcd = bcd >= 1.618 && bcd <= 2.618 // 2.0  --> 2.618
    val _xad = xad >= 0.886 && xad <= 1.000 // 1.13
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isAltBat(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab <= 0.382
    val _abc = abc >= 0.382 && abc <= 0.886
    val _bcd = bcd >= 2.0 && bcd <= 3.618
    val _xad = xad <= 1.13
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isButterfly(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab <= 0.786
    val _abc = abc >= 0.382 && abc <= 0.886
    val _bcd = bcd >= 1.618 && bcd <= 2.618
    val _xad = xad >= 1.27 && xad <= 1.618
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isAntiButterfly(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.236 && xab <= 0.886 // 0.382 - 0.618
    val _abc = abc >= 1.130 && abc <= 2.618 // 1.130 - 2.618
    val _bcd = bcd >= 1.000 && bcd <= 1.382 // 1.27
    val _xad = xad >= 0.500 && xad <= 0.886 // 0.618 - 0.786
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isABCD(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _abc = abc >= 0.382 && abc <= 0.886
    val _bcd = bcd >= 1.13 && bcd <= 2.618
    _abc && _bcd && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isGartley(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.5 && xab <= 0.618 // 0.618
    val _abc = abc >= 0.382 && abc <= 0.886
    val _bcd = bcd >= 1.13 && bcd <= 2.618
    val _xad = xad >= 0.75 && xad <= 0.875 // 0.786
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isAntiGartley(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.500 && xab <= 0.886 // 0.618 -> 0.786
    val _abc = abc >= 1.000 && abc <= 2.618 // 1.130 -> 2.618
    val _bcd = bcd >= 1.500 && bcd <= 5.000 // 1.618
    val _xad = xad >= 1.000 && xad <= 5.000 // 1.272
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isCrab(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.500 && xab <= 0.875 // 0.886
    val _abc = abc >= 0.382 && abc <= 0.886
    val _bcd = bcd >= 2.000 && bcd <= 5.000 // 3.618
    val _xad = xad >= 1.382 && xad <= 5.000 // 1.618
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isAntiCrab(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.250 && xab <= 0.500 // 0.276 -> 0.446
    val _abc = abc >= 1.130 && abc <= 2.618 // 1.130 -> 2.618
    val _bcd = bcd >= 1.618 && bcd <= 2.618 // 1.618 -> 2.618
    val _xad = xad >= 0.500 && xad <= 0.750 // 0.618
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isShark(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.500 && xab <= 0.875 // 0.5 --> 0.886
    val _abc = abc >= 1.130 && abc <= 1.618 //
    val _bcd = bcd >= 1.270 && bcd <= 2.240 //
    val _xad = xad >= 0.886 && xad <= 1.130 // 0.886 --> 1.13
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isAntiShark(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.382 && xab <= 0.875 // 0.446 --> 0.618
    val _abc = abc >= 0.500 && abc <= 1.000 // 0.618 --> 0.886
    val _bcd = bcd >= 1.250 && bcd <= 2.618 // 1.618 --> 2.618
    val _xad = xad >= 0.500 && xad <= 1.250 // 1.130 --> 1.130
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def is5o(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 1.13 && xab <= 1.618
    val _abc = abc >= 1.618 && abc <= 2.24
    val _bcd = bcd >= 0.5 && bcd <= 0.625 // 0.5
    val _xad = xad >= 0.0 && xad <= 0.236 // negative?
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isWolf(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 1.27 && xab <= 1.618
    val _abc = abc >= 0 && abc <= 5
    val _bcd = bcd >= 1.27 && bcd <= 1.618
    val _xad = xad >= 0.0 && xad <= 5
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isHnS(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 2.0 && xab <= 10
    val _abc = abc >= 0.90 && abc <= 1.1
    val _bcd = bcd >= 0.236 && bcd <= 0.88
    val _xad = xad >= 0.90 && xad <= 1.1
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isConTria(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 0.382 && xab <= 0.618
    val _abc = abc >= 0.382 && abc <= 0.618
    val _bcd = bcd >= 0.382 && bcd <= 0.618
    val _xad = xad >= 0.236 && xad <= 0.764
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }

  def isExpTria(_mode: Num, xab: Num, xad: Num, abc: Num, bcd: Num, d: Num, c: Num): Boolean = {
    val _xab = xab >= 1.236 && xab <= 1.618
    val _abc = abc >= 1.000 && abc <= 1.618
    val _bcd = bcd >= 1.236 && bcd <= 2.000
    val _xad = xad >= 2.000 && xad <= 2.236
    _xab && _abc && _bcd && _xad && (if (_mode == numFunction(1)) d < c else d > c)
  }
}

object ZigZagPatterns {
  implicit class NumOps(num: Num) {
    def >=(other: Num): Boolean = num.isGreaterThanOrEqual(other)
    def >(other: Num): Boolean = num.isGreaterThan(other)
    def <=(other: Num): Boolean = num.isLessThanOrEqual(other)
    def <(other: Num): Boolean = num.isLessThan(other)
    def ==(other: Num): Boolean = num.isEqual(other)
    def -(other: Num): Num = num.minus(other)
    def +(other: Num): Num = num.plus(other)
    def /(other: Num): Num = num.dividedBy(other)
    def *(other: Num): Num = num.multipliedBy(other)
  }

  implicit class BooleanOps(value: Boolean) {
    def or(other: Boolean): Boolean = value || other
    def and(other: Boolean): Boolean = value && other
  }

  def abs(num: Num): Num = num.abs()

  implicit def DoubleToNum(double: Double)(implicit numFunction: Double => Num): Num = numFunction(double)
}
