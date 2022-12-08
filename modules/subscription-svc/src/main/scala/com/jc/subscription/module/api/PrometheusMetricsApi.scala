package com.jc.subscription.module.api

import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response, Status}
import zio.Task
import zio.metrics.connectors.prometheus.PrometheusPublisher

object PrometheusMetricsApi {

  def httpRoutes(prometheusPublisher: PrometheusPublisher): HttpRoutes[Task] = {
    import zio.interop.catz._
    val dsl = Http4sDsl[Task]
    import dsl._
    HttpRoutes.of[Task] { case GET -> Root / "metrics" =>
      prometheusPublisher.get.map { res =>
        Response(Status.Ok).withEntity(res)
      }
    }
  }
}
