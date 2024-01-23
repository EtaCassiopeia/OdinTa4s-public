package io.odin.avro

import com.sksamuel.avro4s.RecordFormat
import org.apache.avro.Schema

trait AvroRecordMeta[T <: Product] {
  def recordFormat: RecordFormat[T]
  def schema: Schema
}

object AvroRecordMeta {

  def apply[T <: Product](implicit meta: AvroRecordMeta[T]): AvroRecordMeta[T] = meta

}
