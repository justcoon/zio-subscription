package com.jc.subscription.module.api

import com.jc.auth.JwtAuthenticator
import com.jc.auth.api.GrpcJwtAuth
import com.jc.subscription.domain.proto.{GetSubscriptionReq, GetSubscriptionRes}
import com.jc.subscription.domain.proto.ZioSubscription.RCSubscriptionApiService
import io.grpc.Status
import scalapb.zio_grpc.RequestContext
import zio.{Has, ZIO, ZLayer}

object SubscriptionGrpcApiHandler {

  def toStatus(e: Throwable): Status = {
    Status.INTERNAL.withDescription(e.getMessage)
  }

  final case class LiveSubscriptionApiService(jwtAuthenticator: JwtAuthenticator.Service)
      extends RCSubscriptionApiService[Any] {

    override def getSubscription(
      request: GetSubscriptionReq): ZIO[Any with Has[RequestContext], Status, GetSubscriptionRes] = ???
  }

  val live: ZLayer[JwtAuthenticator, Nothing, SubscriptionGrpcApiHandler] =
    ZLayer.fromService[JwtAuthenticator.Service, RCSubscriptionApiService[Any]] { jwtAuth =>
      LiveSubscriptionApiService(jwtAuth)
    }
}
