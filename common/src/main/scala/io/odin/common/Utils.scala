package io.odin.common

import java.io.File
import java.math.MathContext
import java.nio.file.attribute.{BasicFileAttributes, FileTime}
import java.nio.file.{Files, Paths}
import java.time.format.DateTimeFormatter
import java.time._
import java.util
import java.util.Comparator
import scala.util.Try

object Utils {

  val mathContextPrecisionTwo = new MathContext(2)
  val mathContextPrecisionFive = new MathContext(5)

  private val comparator: Comparator[File] = java.util.Comparator.comparing[File, FileTime](
    new java.util.function.Function[File, FileTime] {
      override def apply(file: File) =
        Files.readAttributes(Paths.get(file.toURI), classOf[BasicFileAttributes]).creationTime()
    }
  )

  def recursiveListFiles(f: File): Array[File] = {
    val allFiles = f.listFiles
    val filteredFiles = allFiles
      .filter(_.isFile)
      .filter(file => file.getName.endsWith(".log") || file.getName.endsWith(".avro")) ++ allFiles
      .filter(_.isDirectory)
      .flatMap(recursiveListFiles)

    util.Arrays.sort(filteredFiles, comparator)
    filteredFiles
  }

  def timestampToLocalDateTime(timestamp: Long): LocalDateTime =
    Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDateTime

  def currentTimestampUTC: Long =
    localDatetimeToEpoch(LocalDateTime.now(Clock.systemUTC()))

  def localDatetimeToInstant(dateTime: LocalDateTime): Instant =
    dateTime.toInstant(ZoneOffset.UTC)

  def localDatetimeToEpoch(dateTime: LocalDateTime): Long =
    localDatetimeToInstant(dateTime).toEpochMilli

  def localDateTimeToDate(dateTime: LocalDateTime) =
    java.util.Date.from(localDatetimeToInstant(dateTime))

  def toLocalDateTime(value: String, dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME) = {
    Try(LocalDateTime.parse(value, dateTimeFormatter)).recoverWith {
      case _ => Try(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay())
    }.toOption
  }

  def calculateFee(amount: BigDecimal, price: BigDecimal): BigDecimal = {
    //Taker fee 0.04
    //Maker fee 0.02

    //ex. Trading price: 18,665.77    Executed: 0.330   Fee: 2.46388164
    (amount * price * 0.04) / 100
  }
}
