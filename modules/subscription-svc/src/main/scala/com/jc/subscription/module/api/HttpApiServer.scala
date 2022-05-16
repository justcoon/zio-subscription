package com.jc.subscription.module.api

import com.jc.subscription.model.config.HttpApiConfig
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.middleware.{Logger => HttpServerLogger}
import org.http4s.server.{Router, Server}
import org.http4s.implicits._
import zio.interop.catz._
import zio.{RIO, UIO, ZIO, ZLayer}

import eu.timepit.refined.auto._

object HttpApiServer {

  type ServerEnv = Any

  private def isReady(): UIO[Boolean] = {
    ZIO.succeed(true)
  }

  private def httpRoutes(): HttpRoutes[RIO[ServerEnv, *]] =
    Router[RIO[ServerEnv, *]](
      "/" -> HealthCheckApi.httpRoutes[ServerEnv](isReady)
    )

  private def httpApp(): HttpApp[RIO[ServerEnv, *]] =
    HttpServerLogger.httpApp[RIO[ServerEnv, *]](true, true)(httpRoutes().orNotFound)

  def make(config: HttpApiConfig): ZLayer[ServerEnv, Throwable, Server] = {
    ZLayer.scoped(
      BlazeServerBuilder[RIO[ServerEnv, *]]
        .bindHttp(config.port, config.address)
        .withHttpApp(httpApp())
        .resource
        .toScopedZIO
    )
  }

  val layer: ZLayer[HttpApiConfig with ServerEnv, Throwable, Server] = {
    val res = for {
      config <- ZIO.service[HttpApiConfig]
      server <- BlazeServerBuilder[RIO[ServerEnv, *]]
        .bindHttp(config.port, config.address)
        .withHttpApp(httpApp())
        .resource
        .toScopedZIO
    } yield server
    ZLayer.scoped(res)
  }

}
