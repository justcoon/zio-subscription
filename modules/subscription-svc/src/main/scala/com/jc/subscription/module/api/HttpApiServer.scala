package com.jc.subscription.module.api

import com.jc.subscription.model.config.HttpApiConfig
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.middleware.{Logger => HttpServerLogger}
import org.http4s.server.{Router, Server}
import org.http4s.implicits._
import zio.interop.catz._
import zio.{Task, UIO, ZIO, ZLayer}
import eu.timepit.refined.auto._

object HttpApiServer {

  private def isReady(): UIO[Boolean] = {
    ZIO.succeed(true)
  }

  private def httpRoutes(): HttpRoutes[Task] =
    Router[Task](
      "/" -> HealthCheckApi.httpRoutes(isReady)
    )

  private def httpApp(): HttpApp[Task] =
    HttpServerLogger.httpApp[Task](true, true)(httpRoutes().orNotFound)

  def make(config: HttpApiConfig): ZLayer[Any, Throwable, Server] = {
    ZLayer.scoped(
      BlazeServerBuilder[Task]
        .bindHttp(config.port, config.address)
        .withHttpApp(httpApp())
        .resource
        .toScopedZIO
    )
  }

  val layer: ZLayer[HttpApiConfig, Throwable, Server] = {
    val res = for {
      config <- ZIO.service[HttpApiConfig]
      server <- BlazeServerBuilder[Task]
        .bindHttp(config.port, config.address)
        .withHttpApp(httpApp())
        .resource
        .toScopedZIO
    } yield server
    ZLayer.scoped(res)
  }

}
