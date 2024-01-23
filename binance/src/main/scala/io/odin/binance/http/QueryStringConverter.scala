package io.odin.binance.http

import enumeratum.EnumEntry
import enumeratum.values.StringEnumEntry
import io.odin.binance.OptionEx
import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}

trait QueryStringConverter[T] {
  def to(t: T): String
}

object QueryStringConverter {
  def apply[T: QueryStringConverter]: QueryStringConverter[T] = implicitly

  implicit val deriveHNil: QueryStringConverter[HNil] = (_: HNil) => ""

  implicit def optionConverter[T: QueryStringConverter]: QueryStringConverter[Option[T]] =
    (obj: Option[T]) =>
      obj
        .map { v =>
          QueryStringConverter[T].to(v)
        }
        .getOrElse("")

  implicit val stringConverter: QueryStringConverter[String] = (obj: String) => obj

  implicit val intConverter: QueryStringConverter[Int] = (obj: Int) => obj.toString

  implicit val longConverter: QueryStringConverter[Long] = (obj: Long) => obj.toString

  implicit val booleanConverter: QueryStringConverter[Boolean] = (obj: Boolean) => obj.toString

  implicit val bigDecimalConverter: QueryStringConverter[BigDecimal] = (obj: BigDecimal) => obj.toString

  implicit def enumEntryConverter[T <: EnumEntry](): QueryStringConverter[T] = (obj: T) => obj.entryName

  implicit def stringEnumEntryConverter[T <: StringEnumEntry](): QueryStringConverter[T] = (obj: T) => obj.value

  implicit def deriveHCons[K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    scv: Lazy[QueryStringConverter[H]],
    sct: QueryStringConverter[T]
  ): QueryStringConverter[FieldType[K, H] :: T] = {
    case h :: HNil =>
      val value = scv.value.to(h)
      OptionEx.when(!value.isEmpty)(value).map(v => witness.value.name + "=" + v).getOrElse("")
    case h :: t =>
      val value = scv.value.to(h)
      OptionEx.when(!value.isEmpty)(value).map(v => witness.value.name + "=" + v + "&").getOrElse("") + sct.to(t)
  }

  implicit def deriveClass[A, Repr](implicit
    gen: LabelledGeneric.Aux[A, Repr],
    conv: Lazy[QueryStringConverter[Repr]]
  ): QueryStringConverter[A] = (a: A) => conv.map(_.to(gen.to(a))).value

}
