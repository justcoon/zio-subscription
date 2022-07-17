package com.jc.subscription.module.api

import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.dsl.Http4sDsl
import zio.Task

object HealthCheckApi {

  def httpRoutes(isReady: () => Task[Boolean]): HttpRoutes[Task] = {
    import zio.interop.catz._
    val dsl = Http4sDsl[Task]
    import dsl._
    HttpRoutes.of[Task] {
      case GET -> Root / "ready" =>
        isReady().map { res =>
          if (res) {
            Response(Status.Ok).withEntity("OK")
          } else {
            Response(Status.ServiceUnavailable)
          }
        }
      case GET -> Root / "alive" =>
        Ok("OK")
    }
  }
}
