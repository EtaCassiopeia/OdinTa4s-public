import Dependencies.Libraries.{http4sModules, zioConfigModules}
import Dependencies._
import Settings._

lazy val common = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-common",
    libraryDependencies ++= Seq(
      Libraries.betterFiles,
      Libraries.shapeless,
      Libraries.Test.scalaTest
    )
  )

lazy val avro = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-avro",
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.zioLogging,
      Libraries.zioLoggingSlf4j,
      Libraries.avro4s,
      Libraries.disruptorAvroWriter
    ) ++ Libraries.log4jModules
  )

lazy val binance = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-binance",
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.zioLogging,
      Libraries.zioLoggingSlf4j,
      Libraries.quickLens,
      Libraries.scalaUri,
      Libraries.avro4s
    ) ++ Libraries.enumeratumModules ++ Libraries.circeModules ++ Libraries.sttpModules
  )
  .dependsOn(common, avro)

lazy val cryptoml = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-crypto-ml",
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.zioLogging,
      Libraries.zioLoggingSlf4j,
      Libraries.Test.scalaTestFunSpec,
      Libraries.Test.scalaTestMatchers
    ) ++ Libraries.enumeratumModules ++ Libraries.circeModules ++ Libraries.sttpModules
  )
  .dependsOn(common, avro)

lazy val ta4s = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-ta4s",
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.zioLogging,
      Libraries.zioLoggingSlf4j,
      Libraries.ta4j,
      Libraries.avro4s,
      Libraries.betterFiles,
      Libraries.disruptorAvroWriter
      //Libraries.scalaChart,
      //Libraries.jFreeSVG,
    )
  )
  .dependsOn(common, binance, avro)

lazy val telegram = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-telegram",
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.zioCats,
      Libraries.zioMacros,
      Libraries.canoe
    )
  )
  .settings(higherKinds)

lazy val trader = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-trader",
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.pureConfig,
      Libraries.ta4j,
      Libraries.canoe,
      Libraries.zioCats
    ),
    mainClass in Compile := Some("io.odin.trader.OdinTrader")
  )
  .settings(
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "io" / "odin" / "trader" / "AppInfo.scala"
      val timeStamp = java.time.ZonedDateTime
        .now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
      IO.write(
        file,
        s"""
           |package io.odin.trader
           |object AppInfo {
           |  val appName : String = "${name.value}"
           |  val appVersion : String = "${version.value}-$timeStamp"
           |}""".stripMargin
      )
      Seq(file)
    }.taskValue
  )
  .dependsOn(binance, ta4s, telegram, common, avro)
  .settings(dockerSettings: _*)
  .enablePlugins(LauncherJarPlugin, DockerPlugin)

lazy val mltrader = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-ml-trader",
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.pureConfig,
      Libraries.ta4j,
      Libraries.canoe,
      Libraries.zioCats
    ),
    mainClass in Compile := Some("io.odin.ml.trader.OdinMLTrader")
  )
  .settings(
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "io" / "odin" / "ml" / "trader" / "AppInfo.scala"
      val timeStamp = java.time.ZonedDateTime
        .now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
      IO.write(
        file,
        s"""
           |package io.odin.ml.trader
           |object AppInfo {
           |  val appName : String = "${name.value}"
           |  val appVersion : String = "${version.value}-$timeStamp"
           |}""".stripMargin
      )
      Seq(file)
    }.taskValue
  )
  .dependsOn(binance, telegram, common, cryptoml, ta4s)
  .settings(mlDockerSettings: _*)
  .enablePlugins(LauncherJarPlugin, DockerPlugin)

lazy val tvtrader = project
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    name := "odin-trading-view-trader",
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.pureConfig,
      Libraries.ta4j,
      Libraries.canoe,
      Libraries.zioCats
    ) ++ http4sModules ++ zioConfigModules,
    mainClass in Compile := Some("io.odin.tv.trader.OdinTVTraderSignal")
  )
  .settings(
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "io" / "odin" / "tv" / "trader" / "AppInfo.scala"
      val timeStamp = java.time.ZonedDateTime
        .now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
      IO.write(
        file,
        s"""
           |package io.odin.tv.trader
           |object AppInfo {
           |  val appName : String = "${name.value}"
           |  val appVersion : String = "${version.value}-$timeStamp"
           |}""".stripMargin
      )
      Seq(file)
    }.taskValue
  )
  .dependsOn(binance, telegram, common, cryptoml, ta4s)
  .settings(tvDockerSettings: _*)
  .enablePlugins(LauncherJarPlugin, DockerPlugin)

lazy val root = (project in file("."))
  .aggregate(common, avro, ta4s, binance, cryptoml, telegram, trader, mltrader, tvtrader)
  .settings(
    name := "odin-algo-trading"
  )
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
