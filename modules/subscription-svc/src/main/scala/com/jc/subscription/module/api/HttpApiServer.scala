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
import zio.metrics.connectors.prometheus.PrometheusPublisher

object HttpApiServer {

  private def isReady(): UIO[Boolean] = {
    ZIO.succeed(true)
  }

  private def httpRoutes(prometheusPublisher: PrometheusPublisher): HttpRoutes[Task] =
    Router[Task](
      "/" -> HealthCheckApi.httpRoutes(isReady),
      "/" -> PrometheusMetricsApi.httpRoutes(prometheusPublisher)
    )

  private def httpApp(prometheusPublisher: PrometheusPublisher): HttpApp[Task] =
    HttpServerLogger.httpApp[Task](true, true)(httpRoutes(prometheusPublisher).orNotFound)

  def make(config: HttpApiConfig, prometheusPublisher: PrometheusPublisher): ZLayer[Any, Throwable, Server] = {
    ZLayer.scoped(
      BlazeServerBuilder[Task]
        .bindHttp(config.port, config.address)
        .withHttpApp(httpApp(prometheusPublisher))
        .resource
        .toScopedZIO
    )
  }

  val layer: ZLayer[HttpApiConfig with PrometheusPublisher, Throwable, Server] = {
    val res = for {
      config <- ZIO.service[HttpApiConfig]
      prometheusPublisher <- ZIO.service[PrometheusPublisher]
      server <- BlazeServerBuilder[Task]
        .bindHttp(config.port, config.address)
        .withHttpApp(httpApp(prometheusPublisher))
        .resource
        .toScopedZIO
    } yield server
    ZLayer.scoped(res)
  }

}
