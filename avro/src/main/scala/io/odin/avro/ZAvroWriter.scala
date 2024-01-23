package io.odin.avro

import com.aol.advertising.vulcan.api
import com.aol.advertising.vulcan.api.{AvroWriter, AvroWriterBuilder}
import com.aol.advertising.vulcan.rolling.TimeAndSizeBasedRollingPolicyConfig
import zio._

case class AvroWriteConfig(fileName: String, rollingPolicyConfig: TimeAndSizeBasedRollingPolicyConfig)

object ZAvroWriter {

  type ZAvroWriter = Has[Service]

  trait Service {
    def write[T <: Product: AvroRecordMeta](record: T): ZIO[Any, Throwable, Unit]
  }

  val live: ZLayer[Has[AvroWriter], Throwable, ZAvroWriter] = {
    ZLayer.fromService { avroWriter =>
      new Service {
        override def write[T <: Product: AvroRecordMeta](record: T): ZIO[Any, Throwable, Unit] = {
          ZIO.effect(avroWriter.write(AvroRecordMeta[T].recordFormat.to(record)))
        }
      }
    }
  }

  def avroWriter[T <: Product: AvroRecordMeta]: ZLayer[Has[AvroWriteConfig], Throwable, Has[api.AvroWriter]] =
    ZManaged.fromAutoCloseable {
      for {
        writerConfig <- ZIO.environment[Has[AvroWriteConfig]]
        managed <- ZIO.effect(
          AvroWriterBuilder
            .startCreatingANewWriter()
            .thatWritesTo(writerConfig.get.fileName)
            .thatWritesRecordsOf(AvroRecordMeta[T].schema)
            .withDefaultRollingPolicyConfiguration(writerConfig.get.rollingPolicyConfig)
            .createNewWriter()
        )
      } yield managed
    }.toLayer

  def avroWriterConfigLayer(avroWriterConfig: AvroWriteConfig): ULayer[Has[AvroWriteConfig]] =
    ZLayer.succeed(avroWriterConfig)

  def write[T <: Product: AvroRecordMeta](record: T): ZIO[ZAvroWriter, Throwable, Unit] =
    ZIO.accessM(_.get.write(record))

}
