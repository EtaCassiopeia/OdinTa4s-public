import Dependencies._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.jar.LauncherJarPlugin.autoImport.packageJavaLauncherJar
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{Docker, dockerPermissionStrategy}
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{parallelExecution, _}
import sbt._
import sbt.util.Level

import scala.language.postfixOps

object Settings {

  lazy val compilerSettings =
    Seq(
      scalaVersion := "2.13.4",
      javacOptions ++= Seq("-source", "11", "-target", "11"),
      scalacOptions := Seq(
        "-Ymacro-annotations",
        "-deprecation",
        "-encoding",
        "utf-8",
        "-explaintypes",
        "-feature",
        "-unchecked",
        "-language:postfixOps",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Xcheckinit",
        "-Xfatal-warnings"
      ),
      logLevel := Level.Info,
      version := "0.8.0",
      scalafmtOnCompile := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )

  lazy val sbtSettings =
    Seq(
      fork := true,
      parallelExecution in Test := false,
      cancelable in Global := true
    )

  lazy val commonSettings =
    compilerSettings ++
      sbtSettings ++ Seq(
      organization := "io.odin",
      resolvers ++= Seq(
        Resolver.mavenLocal,
        "Confluent".at("https://packages.confluent.io/maven/"),
        "jitpack".at("https://jitpack.io"),
        Resolver.jcenterRepo
      )
    )

  val higherKinds = addCompilerPlugin("org.typelevel" %% "kind-projector" % Versions.kindProjector)

  lazy val dockerSettings =
    Seq(
      dockerBaseImage := "gcr.io/distroless/java-debian10:11",
      packageName in Docker := "odin-trader/trader",
      maintainer in Docker := "Mohsen Zainalpour",
      packageSummary := "OdinTrader",
      packageDescription := "Algorithmic trading application",
      daemonUserUid in Docker := None,
      daemonUser in Docker := "root",
      dockerPermissionStrategy := DockerPermissionStrategy.None,
      dockerEntrypoint := Seq("java", "-jar"),
      dockerCmd := Seq(s"/opt/docker/lib/${(artifactPath in packageJavaLauncherJar).value.getName}"),
      dockerRepository := Some("eu.gcr.io")
    )

  lazy val mlDockerSettings =
    Seq(
      dockerBaseImage := "gcr.io/distroless/java-debian10:11",
      packageName in Docker := "odin-trader/ml-trader",
      maintainer in Docker := "Mohsen Zainalpour",
      packageSummary := "OdinMLTrader",
      packageDescription := "ML trading application",
      daemonUserUid in Docker := None,
      daemonUser in Docker := "root",
      dockerPermissionStrategy := DockerPermissionStrategy.None,
      dockerEntrypoint := Seq("java", "-jar"),
      dockerCmd := Seq(s"/opt/docker/lib/${(artifactPath in packageJavaLauncherJar).value.getName}"),
      dockerRepository := Some("eu.gcr.io")
    )

  lazy val tvDockerSettings =
    Seq(
      dockerBaseImage := "gcr.io/distroless/java-debian10:11",
      packageName in Docker := "odin-trader/tv-trader",
      maintainer in Docker := "Mohsen Zainalpour",
      packageSummary := "OdinTVTrader",
      packageDescription := "TradingView trading application",
      daemonUserUid in Docker := None,
      daemonUser in Docker := "root",
      dockerPermissionStrategy := DockerPermissionStrategy.None,
      dockerEntrypoint := Seq("java", "-jar"),
      dockerCmd := Seq(s"/opt/docker/lib/${(artifactPath in packageJavaLauncherJar).value.getName}"),
      dockerRepository := Some("eu.gcr.io")
    )

}
