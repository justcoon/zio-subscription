package com.jc.subscription.module.api

import com.jc.logging.api.LoggingSystemGrpcApiHandler
import com.jc.logging.proto.ZioLoggingSystemApi.RCLoggingSystemApiService
import com.jc.subscription.domain.proto.ZioSubscriptionApi.RCSubscriptionApiService
import com.jc.subscription.model.config.HttpApiConfig
import scalapb.zio_grpc.{ScopedServer, Server => GrpcServer, ServiceList => GrpcServiceList}
import io.grpc.ServerBuilder
import zio.{ZIO, ZLayer}
import eu.timepit.refined.auto._

object GrpcApiServer {

  val layer
    : ZLayer[HttpApiConfig with LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler, Throwable, GrpcServer] = {
    val res = for {
      config <- ZIO.service[HttpApiConfig]
      server <- ScopedServer.fromServiceList(
        ServerBuilder.forPort(config.port),
        GrpcServiceList.empty
          .addFromEnvironment[RCLoggingSystemApiService]
          .addFromEnvironment[RCSubscriptionApiService])
    } yield server
    ZLayer.scoped(res)
  }
}
