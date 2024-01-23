package io.odin.binance.client.model

import java.time.{Duration, LocalDateTime, ZoneOffset}
import io.circe.Decoder
import CandlestickRequest.Interval
import com.sksamuel.avro4s.{AvroSchema, RecordFormat, ScalePrecision}
import io.odin.avro.AvroRecordMeta
import io.odin.binance.http.QueryStringConverter
import org.apache.avro.Schema

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode

final case class CandlestickRequest(
  symbol: String,
  interval: Interval,
  startTime: Option[Long] = None,
  endTime: Option[Long] = Some(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli),
  limit: Int = 500
)

object CandlestickRequest {
  import enumeratum.values._

  sealed abstract class Interval(val value: String, name: String) extends StringEnumEntry {
    def finiteDuration: FiniteDuration
    def duration: Duration
  }

  case object Interval extends StringEnum[Interval] with StringCirceEnum[Interval] {
    val values = findValues

    case object OneMinute extends Interval(value = "1m", name = "OneMinute") {
      val finiteDuration: FiniteDuration = 1.minute
      val duration: Duration = Duration.ofMinutes(1)
    }
    case object FiveMinutes extends Interval(value = "5m", name = "FiveMinutes") {
      val finiteDuration: FiniteDuration = 5.minute
      val duration: Duration = Duration.ofMinutes(5)
    }
    case object FifteenMinutes extends Interval(value = "15m", name = "FifteenMinutes") {
      val finiteDuration: FiniteDuration = 15.minute
      val duration: Duration = Duration.ofMinutes(15)
    }
    case object ThirtyMinutes extends Interval(value = "30m", name = "ThirtyMinutes") {
      val finiteDuration: FiniteDuration = 30.minute
      val duration: Duration = Duration.ofMinutes(30)
    }
    case object OneHour extends Interval(value = "1h", name = "OneHour") {
      val finiteDuration: FiniteDuration = 1.hour
      val duration: Duration = Duration.ofHours(1)
    }

  }

  implicit val intervalQueryStringConverter: QueryStringConverter[Interval] =
    QueryStringConverter.stringEnumEntryConverter[Interval]()
}

final case class Candlestick(
  openTime: Long,
  open: BigDecimal,
  high: BigDecimal,
  low: BigDecimal,
  close: BigDecimal,
  volume: BigDecimal,
  closeTime: Long,
  quoteAssetVolume: BigDecimal,
  numberOfTrades: BigDecimal,
  takerBuyBaseAssetVolume: BigDecimal,
  takerBuyQuoteAssetVolume: BigDecimal,
  ignore: BigDecimal
)

object Candlestick {
  implicit val decoder: Decoder[Candlestick] =
    Decoder.instance { cursor =>
      val openTimeCursor = cursor.downArray

      for {
        openTime <- openTimeCursor.as[Long]
        openCursor = openTimeCursor.right
        open <- openCursor.as[BigDecimal]
        highC = openCursor.right
        high <- highC.as[BigDecimal]
        lowC = highC.right
        low <- lowC.as[BigDecimal]
        closeC = lowC.right
        close <- closeC.as[BigDecimal]
        volumeC = closeC.right
        volume <- volumeC.as[BigDecimal]
        closeTimeC = volumeC.right
        closeTime <- closeTimeC.as[Long]
        quoteAssetVolumeC = closeTimeC.right
        quoteAssetVolume <- quoteAssetVolumeC.as[BigDecimal]
        numberOfTradesC = quoteAssetVolumeC.right
        numberOfTrades <- numberOfTradesC.as[BigDecimal]
        takerBuyBaseAssetVolumeC = numberOfTradesC.right
        takerBuyBaseAssetVolume <- takerBuyBaseAssetVolumeC.as[BigDecimal]
        takerBuyQuoteAssetVolumeC = takerBuyBaseAssetVolumeC.right
        takerBuyQuoteAssetVolume <- takerBuyQuoteAssetVolumeC.as[BigDecimal]
        ignoreC = takerBuyQuoteAssetVolumeC.right
        ignore <- ignoreC.as[BigDecimal]
      } yield Candlestick(
        openTime,
        open,
        high,
        low,
        close,
        volume,
        closeTime,
        quoteAssetVolume,
        numberOfTrades,
        takerBuyBaseAssetVolume,
        takerBuyQuoteAssetVolume,
        ignore
      )
    }

  import ScalePrecision._
//  implicit val sp: ScalePrecision = ScalePrecision(4, 20)
  implicit val roundingMode: BigDecimal.RoundingMode.Value = RoundingMode.HALF_UP

  implicit val candleStickAvroRecordMeta: AvroRecordMeta[Candlestick] = new AvroRecordMeta[Candlestick] {
    override def recordFormat: RecordFormat[Candlestick] = RecordFormat[Candlestick]
    override def schema: Schema = AvroSchema[Candlestick]
  }
}
