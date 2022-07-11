package com.jc.subscription.module.api

import com.jc.logging.api.LoggingSystemGrpcApiHandler
import com.jc.logging.proto.ZioLoggingSystemApi.RCLoggingSystemApiService
import com.jc.subscription.domain.proto.ZioSubscriptionApi.RCSubscriptionApiService
import com.jc.subscription.model.config.HttpApiConfig
import scalapb.zio_grpc.{
  ManagedServer,
  Server => GrpcServer,
  ServerLayer => GrpcServerLayer,
  ServiceList => GrpcServiceList
}
import io.grpc.ServerBuilder
import zio.{ZIO, ZLayer}
import eu.timepit.refined.auto._

object GrpcApiServer {

  def make(config: HttpApiConfig)
    : ZLayer[LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler, Throwable, GrpcServer] = {
    GrpcServerLayer.fromServiceList(
      ServerBuilder.forPort(config.port),
      GrpcServiceList.access[RCLoggingSystemApiService[Any]].access[RCSubscriptionApiService[Any]])
  }

  val layer
    : ZLayer[HttpApiConfig with LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler, Throwable, GrpcServer] = {
    val res = for {
      config <- ZIO.service[HttpApiConfig]
      server <- ManagedServer.fromServiceList(
        ServerBuilder.forPort(config.port),
        GrpcServiceList.access[RCLoggingSystemApiService[Any]].access[RCSubscriptionApiService[Any]])
    } yield server
    ZLayer.scoped(res)
  }
}
