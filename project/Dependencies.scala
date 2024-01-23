import sbt._

object Dependencies {

  object Versions {
    val zioVersion = "1.0.5"
    val zioCatsVersion = "2.3.1.0"
    val zioLoggingVersion = "0.5.7"
    val pureConfigVersion = "0.14.0"
    val circeVersion = "0.13.0"
    val sttpVersion = "2.2.9"
    val enumeratumVersion = "1.6.1"
    val quickLensVersion = "1.6.1"
    val sqoobaVersion = "1.1.0"
    val ta4jVersion = "0.13"
    val avroWriterVersion = "1.1.0"
    val avro4sVersion = "4.0.0"
    val scalaUriVersion = "2.2.2"
    val canoeVersion = "0.5.1"
    val log4jVersion = "2.13.1"
    val kindProjector = "0.10.3"
    val shapelessVersion = "2.3.3"
    val scalaTestVersion = "3.2.2"
    val http4sVersion = "0.21.3"
    val zioConfigVersion = "1.0.2"
  }

  object Libraries {

    import Versions._

    val zio = "dev.zio" %% "zio" % zioVersion
    val zioLogging = "dev.zio" %% "zio-logging" % zioLoggingVersion
    val zioCats = ("dev.zio" %% "zio-interop-cats" % zioCatsVersion).excludeAll(ExclusionRule("dev.zio"))
    val zioMacros = "dev.zio" %% "zio-macros" % zioVersion
    val zioLoggingSlf4j = "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion

    private val log4j: String => ModuleID = artifact => "org.apache.logging.log4j" % artifact % log4jVersion

    val log4jModules: Seq[sbt.ModuleID] = Seq("log4j-api", "log4j-core", "log4j-slf4j-impl").map(log4j)

    val shapeless = "com.chuusai" %% "shapeless" % shapelessVersion

    private val sttpModule: String => ModuleID = artifact => "com.softwaremill.sttp.client" %% artifact % sttpVersion

    val sttpModules: Seq[sbt.ModuleID] = Seq("core", "async-http-client-backend-zio", "circe").map(sttpModule)

    private val http4s: String => ModuleID = artifact => "org.http4s" %% artifact % http4sVersion
    val http4sModules = Seq("http4s-blaze-server", "http4s-dsl", "http4s-circe").map(http4s)

    private val zioConfig: String => ModuleID = artifact => "dev.zio" %% artifact % zioConfigVersion
    val zioConfigModules =
      Seq("zio-config", "zio-config-magnolia", "zio-config-typesafe").map(zioConfig)

    private val circeModule: String => ModuleID = artifact => "io.circe" %% artifact % circeVersion

    val circeModules: Seq[sbt.ModuleID] =
      Seq("circe-core", "circe-parser", "circe-generic", "circe-generic-extras", "circe-shapes").map(circeModule)

    val enumeratumModules =
      Seq(
        "com.beachape" %% "enumeratum" % enumeratumVersion,
        "com.beachape" %% "enumeratum-circe" % enumeratumVersion
      )

    val ta4j = "org.ta4j" % "ta4j-core" % ta4jVersion
    val disruptorAvroWriter = "com.aol.advertising.vulcan" % "disruptor_avro_writer" % avroWriterVersion
    val avro4s = "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion

    val scalaUri = "io.lemonlabs" %% "scala-uri" % scalaUriVersion

    val scalaChart = "com.github.wookietreiber" %% "scala-chart" % "latest.integration"
    val jFreeSVG = "org.jfree" % "jfreesvg" % "3.4"

    val betterFiles = "com.github.pathikrit" %% "better-files" % "3.9.1"

    val pureConfig = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
    val quickLens = "com.softwaremill.quicklens" %% "quicklens" % quickLensVersion

    val canoe = "org.augustjune" %% "canoe" % canoeVersion
    val zioWeb = "dev.zio" %% "zio-web-core" % "0.0.0+66-c95559f6"

    object Test {
      val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
      val scalaTestFunSpec = "org.scalatest" %% "scalatest-funspec" % scalaTestVersion % "test"
      val scalaTestMatchers = "org.scalatest" %% "scalatest-shouldmatchers" % scalaTestVersion % "test"
    }
  }

}
