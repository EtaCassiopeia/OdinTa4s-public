package io.odin.common

object PrettyCaseClass {
  implicit class PrettyCaseClassOps[T <: Product](product: Product) {
    def toStringWithFields: String = {

      val productFields = product.productElementNames
      val productIterator = product.productIterator

      productFields
        .zip(productIterator.toList)
        .map { case (field, value) => s"$field = $value" }
        .mkString("(", ", ", ")")
    }
  }
}
