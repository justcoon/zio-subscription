package com.jc.subscription.module.api

import com.jc.auth.JwtAuthenticator
import com.jc.auth.api.GrpcJwtAuth
import com.jc.subscription.domain.proto.{
  CreateSubscriptionReq,
  CreateSubscriptionRes,
  GetSubscriptionReq,
  GetSubscriptionRes,
  GetSubscriptionsReq,
  GetSubscriptionsRes,
  RemoveSubscriptionReq,
  RemoveSubscriptionRes
}
import com.jc.subscription.domain.proto.ZioSubscriptionApi.RCSubscriptionApiService
import com.jc.subscription.module.domain.SubscriptionDomain
import io.grpc.Status
import scalapb.zio_grpc.RequestContext
import zio.{Has, ZIO, ZLayer}

object SubscriptionGrpcApiHandler {

  def toStatus(e: Throwable): Status = {
    Status.INTERNAL.withDescription(e.getMessage)
  }

  final class LiveSubscriptionApiService(
    subscriptionService: SubscriptionDomain.Service,
    jwtAuthenticator: JwtAuthenticator.Service)
      extends RCSubscriptionApiService[Any] {

    private val authenticated = GrpcJwtAuth.authenticated(jwtAuthenticator)

    override def getSubscription(
      request: GetSubscriptionReq): ZIO[Any with Has[RequestContext], Status, GetSubscriptionRes] = {
      for {
        _ <- authenticated
        res <- subscriptionService.getSubscription(request).mapError(toStatus)
      } yield res
    }

    override def createSubscription(
      request: CreateSubscriptionReq): ZIO[Any with Has[RequestContext], Status, CreateSubscriptionRes] = {

      for {
        _ <- authenticated
        res <- subscriptionService.createSubscription(request).mapError(toStatus)
      } yield res
    }

    override def removeSubscription(
      request: RemoveSubscriptionReq): ZIO[Any with Has[RequestContext], Status, RemoveSubscriptionRes] = {
      for {
        _ <- authenticated
        res <- subscriptionService.removeSubscription(request).mapError(toStatus)
      } yield res
    }

    override def getSubscriptions(
      request: GetSubscriptionsReq): ZIO[Any with Has[RequestContext], Status, GetSubscriptionsRes] = {
      for {
        _ <- authenticated
        res <- subscriptionService.getSubscriptions(request).mapError(toStatus)
      } yield res
    }
  }

  val live: ZLayer[SubscriptionDomain with JwtAuthenticator, Nothing, SubscriptionGrpcApiHandler] = {
    val res = for {
      jwtAuth <- ZIO.service[JwtAuthenticator.Service]
      service <- ZIO.service[SubscriptionDomain.Service]
    } yield {
      new LiveSubscriptionApiService(service, jwtAuth)
    }
    res.toLayer
  }

}
