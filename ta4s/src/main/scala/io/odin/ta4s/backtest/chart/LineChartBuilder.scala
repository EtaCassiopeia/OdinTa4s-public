//package io.odin.ta4s.backtest.chart
//
//import java.awt.Color
//import java.time.LocalDateTime
//import java.time.format.TextStyle
//import java.util.Locale
//
//import io.odin.ta4s.backtest.common.Utils
//import io.odin.ta4s.backtest.common.Utils._
//import io.odin.ta4s.binance.client.model.Candlestick
//import io.odin.ta4s.indicators.{OdinDailyHighIndicator, OdinDailyLowIndicator, OdinMACDHistogramIndicator}
//import org.jfree.chart.axis.{DateAxis, NumberAxis}
//import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
//import org.jfree.data.time.{Millisecond, RegularTimePeriod}
//import scalax.chart._
//
//import scala.collection.JavaConverters._
//import scala.util.Try
//
//trait RegularTimePeriodBuilder[T] {
//  def timePeriod(value: T): RegularTimePeriod
//
//  def eventDate(value: T): LocalDateTime
//}
//
//object RegularTimePeriodBuilder {
//  def apply[T](implicit regularTimePeriodBuilder: RegularTimePeriodBuilder[T]): RegularTimePeriodBuilder[T] =
//    regularTimePeriodBuilder
//
//  implicit val candleStickRegularTimePeriodBuilder: RegularTimePeriodBuilder[Candlestick] =
//    new RegularTimePeriodBuilder[Candlestick] {
//      override def timePeriod(value: Candlestick): RegularTimePeriod =
//        new Millisecond(localDateTimeToDate(eventDate(value)))
//
//      override def eventDate(value: Candlestick): LocalDateTime = timestampToLocalDateTime(value.openTime)
//    }
//
//  implicit val macdRegularTimePeriodBuilder: RegularTimePeriodBuilder[OdinMACDHistogramIndicator[Candlestick]] =
//    new RegularTimePeriodBuilder[OdinMACDHistogramIndicator[Candlestick]] {
//      override def timePeriod(value: OdinMACDHistogramIndicator[Candlestick]): RegularTimePeriod =
//        new Millisecond(localDateTimeToDate(eventDate(value)))
//
//      override def eventDate(value: OdinMACDHistogramIndicator[Candlestick]): LocalDateTime = value.getLastBarTime
//    }
//
//  implicit val dailyHighRegularTimePeriodBuilder: RegularTimePeriodBuilder[OdinDailyHighIndicator[Candlestick]] =
//    new RegularTimePeriodBuilder[OdinDailyHighIndicator[Candlestick]] {
//      override def timePeriod(value: OdinDailyHighIndicator[Candlestick]): RegularTimePeriod =
//        new Millisecond(localDateTimeToDate(eventDate(value)))
//
//      override def eventDate(value: OdinDailyHighIndicator[Candlestick]): LocalDateTime = value.getLastBarTime
//    }
//
//  implicit val dailyLowRegularTimePeriodBuilder: RegularTimePeriodBuilder[OdinDailyLowIndicator[Candlestick]] =
//    new RegularTimePeriodBuilder[OdinDailyLowIndicator[Candlestick]] {
//      override def timePeriod(value: OdinDailyLowIndicator[Candlestick]): RegularTimePeriod =
//        new Millisecond(localDateTimeToDate(eventDate(value)))
//
//      override def eventDate(value: OdinDailyLowIndicator[Candlestick]): LocalDateTime = value.getLastBarTime
//    }
//
//}
//
//abstract class ChartBuilder(chartName: String, autoSave: Boolean, baseDir: String = "/tmp")
//    extends scalax.chart.module.Charting
//    with AutoCloseable {
//
//  protected val fileName: LocalDateTime => String = { eventDateTime =>
//    s"${eventDateTime.getMonth.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}-${eventDateTime.getDayOfMonth}-${eventDateTime.getHour}.png"
//  }
//
//  private[chart] def clearTimeSeries(timeSeries: TimeSeries*): Unit = timeSeries.foreach(_.clear())
//
//  protected def saveChart(prefix: String, timeSeries: TimeSeries*): Unit = {
//    if (autoSave) {
//      val chart = XYLineChart(List(timeSeries: _*))
//      chart.saveAsPNG(s"$baseDir/$chartName-$prefix-${fileName(getTimeSeriesMaxAge(timeSeries(0)))}")
//      clearTimeSeries(timeSeries: _*)
//    }
//  }
//
//  protected def getTimeSeriesMaxAge(timeSeries: TimeSeries): LocalDateTime =
//    Utils.timestampToLocalDateTime(timeSeries.getDataItem(timeSeries.getItemCount - 1).getPeriod.getEnd.getTime)
//
//  def dailySeries: List[TimeSeries]
//
//  def fullSeries: List[TimeSeries]
//}
//
//class CrossChartBuilder[T: RegularTimePeriodBuilder](
//  chartName: String,
//  firstValueExtractor: T => Double,
//  secondValueExtractor: T => Double,
//  baseDir: String = "/tmp",
//  autoSave: Boolean = true
//) extends ChartBuilder(chartName, autoSave, baseDir) {
//  private val dailyFirstTimeSeries = new TimeSeries("MACDLine")
//  private val dailySecondsTimeSeries = new TimeSeries("Signal")
//  private val fullFirstTimeSeries = new TimeSeries("MACDLine")
//  private val fullSecondTimeSeries = new TimeSeries("Signal")
//
//  def addEvent(event: T): Unit = {
//    val eventDate = RegularTimePeriodBuilder[T].eventDate(event)
//    if (!dailyFirstTimeSeries.isEmpty) {
//      val maxItemAge = getTimeSeriesMaxAge(dailyFirstTimeSeries)
//      if (eventDate.getDayOfYear > maxItemAge.getDayOfYear) {
//        saveChart("Daily", dailySeries: _*)
//      }
//    }
//
//    Try {
//
//      val timePeriod = RegularTimePeriodBuilder[T].timePeriod(event)
//
//      val firstChartValue: Double = firstValueExtractor(event)
//      val secondsChartValue: Double = secondValueExtractor(event)
//
//      dailyFirstTimeSeries.addOrUpdate(timePeriod, firstChartValue)
//      dailySecondsTimeSeries.addOrUpdate(timePeriod, secondsChartValue)
//
//      fullFirstTimeSeries.addOrUpdate(timePeriod, firstChartValue)
//      fullSecondTimeSeries.addOrUpdate(timePeriod, secondsChartValue)
//    }
//
//    ()
//  }
//
//  override def close(): Unit =
//    if (!dailyFirstTimeSeries.isEmpty) {
//      saveChart("Daily", dailySeries: _*)
//      if (!fullFirstTimeSeries.isEmpty)
//        saveChart("AllData", fullSeries: _*)
//
//    }
//
//  override def dailySeries: List[TimeSeries] = List(dailyFirstTimeSeries, dailySecondsTimeSeries)
//
//  override def fullSeries: List[TimeSeries] = List(fullFirstTimeSeries, fullSecondTimeSeries)
//}
//
//class LineChartBuilder[T: RegularTimePeriodBuilder](
//  chartName: String,
//  valueExtractor: T => Double,
//  baseDir: String = "/tmp",
//  autoSave: Boolean = true
//) extends ChartBuilder(chartName, autoSave, baseDir) {
//  private val dailyTimeSeries = new TimeSeries(chartName)
//  private val fullTimeSeries = new TimeSeries(chartName)
//
//  def addEvent(event: T): Unit = {
//    val eventDate = RegularTimePeriodBuilder[T].eventDate(event)
//    if (!dailyTimeSeries.isEmpty) {
//      val maxItemAge = getTimeSeriesMaxAge(dailyTimeSeries)
//      if (eventDate.getDayOfYear > maxItemAge.getDayOfYear) {
//        saveChart("Daily", dailySeries: _*)
//      }
//    }
//    dailyTimeSeries.addOrUpdate(RegularTimePeriodBuilder[T].timePeriod(event), valueExtractor(event))
//    fullTimeSeries.addOrUpdate(RegularTimePeriodBuilder[T].timePeriod(event), valueExtractor(event))
//  }
//
//  override def close(): Unit =
//    if (!dailyTimeSeries.isEmpty) {
//      saveChart("Daily", dailySeries: _*)
//      if (!fullTimeSeries.isEmpty)
//        saveChart("AllData", fullSeries: _*)
//
//    }
//
//  override def dailySeries: List[TimeSeries] = List(dailyTimeSeries)
//
//  override def fullSeries: List[TimeSeries] = List(fullTimeSeries)
//}
//
//class ComboChartSaver(chartName: String, baseDir: String, charts: ChartBuilder*)
//    extends scalax.chart.module.Charting
//    with AutoCloseable {
//
//  private val fileName: LocalDateTime => String = { eventDateTime =>
//    s"${eventDateTime.getMonth.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}-${eventDateTime.getDayOfMonth}-${eventDateTime.getHour}.png"
//  }
//
//  private def getTimeSeriesMaxAge(timeSeries: TimeSeries): LocalDateTime =
//    Utils.timestampToLocalDateTime(timeSeries.getDataItem(timeSeries.getItemCount - 1).getPeriod.getEnd.getTime)
//
//  private def domain[A: ToXYDataset](
//    datasetGroups: List[List[A]]
//  )(implicit theme: ChartTheme = ChartTheme.Default): XYChart = {
//
//    val plot = new XYPlot()
//
//    datasetGroups.zipWithIndex.foreach {
//      case (data, groupIndex) =>
//        val datasets = data.map(ToXYDataset[A].convert)
//
//        val axis = new NumberAxis()
//        axis.setAutoRange(true)
//        axis.setAutoRangeIncludesZero(false)
//        plot.setRangeAxis(groupIndex, axis)
//
//        val domainAxis = new DateAxis()
//        plot.setDomainAxis(groupIndex, domainAxis)
//
//        datasets.zipWithIndex.foreach {
//          case (dataset, datasetIndex) =>
//            val index = groupIndex + datasetIndex
//            plot.setDataset(index, dataset)
//
//            plot.setRenderer(index, new XYLineAndShapeRenderer(true, false))
//
//            plot.mapDatasetToDomainAxes(index, List(groupIndex).asJava)
//            plot.mapDatasetToRangeAxis(index, groupIndex)
//        }
//    }
//
//    XYChart(plot, title = "", legend = true)
//  }
//
//  private def saveChart(prefix: String, timeSeries: List[List[TimeSeries]]): Unit = {
//    val chart = domain(timeSeries)
//    chart.saveAsPNG(s"$baseDir/$chartName-$prefix-${fileName(getTimeSeriesMaxAge(timeSeries.head.head))}")
//
//    timeSeries.foreach(group => group.foreach(_.clear()))
//  }
//
//  def checkAndSave(eventTime: Long): Unit = {
//    val eventDate = Utils.timestampToLocalDateTime(eventTime)
//
//    if (!charts(0).dailySeries.head.isEmpty) {
//      val maxItemAge = getTimeSeriesMaxAge(charts(0).dailySeries.head)
//      if (eventDate.getDayOfYear > maxItemAge.getDayOfYear) {
//        val tsList = charts.map(_.dailySeries).toList
//        saveChart("Daily", tsList)
//        charts.foreach(chart => tsList.map(chart.clearTimeSeries))
//      }
//    }
//  }
//
//  override def close(): Unit = {
//    if (!charts(0).dailySeries.head.isEmpty) {
//      saveChart("Daily", charts.map(_.dailySeries).toList)
//    }
//
//    if (!charts(0).fullSeries.head.isEmpty) {
//      saveChart("AllData", charts.map(_.fullSeries).toList)
//    }
//
//    charts.foreach(_.close())
//  }
//}
