package io.odin.tv.trader

import io.odin.tv.trader.OdinTVTraderSignal.{AppEnvironment, AppTask}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.config.getConfig
import zio.interop.catz._

object Http4Server {

  type Http4Server = Has[Server[AppTask]]

  def createHttp4Server(): ZManaged[AppEnvironment, Throwable, Server[AppTask]] =
    for {
      serverConfig <- getConfig[ServerConfig].toManaged_
      server <- ZManaged.runtime[AppEnvironment].flatMap { implicit runtime: Runtime[AppEnvironment] =>
        BlazeServerBuilder[AppTask]
          .bindHttp(serverConfig.port, serverConfig.host)
          .withHttpApp(Routes.webhookService())
          .resource
          .toManagedZIO
      }
    } yield server

  def createHttp4sLayer(): ZLayer[AppEnvironment, Throwable, Has[Server[AppTask]]] =
    ZLayer.fromManaged {
      for {
        server <- createHttp4Server()
      } yield server
    }

}
