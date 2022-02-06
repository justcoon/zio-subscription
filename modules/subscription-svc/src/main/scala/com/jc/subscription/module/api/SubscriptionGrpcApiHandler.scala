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
import com.jc.subscription.module.repo.SubscriptionRepo
import io.grpc.Status
import scalapb.zio_grpc.RequestContext
import zio.{Has, ZIO, ZLayer}

object SubscriptionGrpcApiHandler {

  def toStatus(e: Throwable): Status = {
    Status.INTERNAL.withDescription(e.getMessage)
  }

  final class LiveSubscriptionApiService(
    subscriptionRepo: SubscriptionRepo.Service,
    jwtAuthenticator: JwtAuthenticator.Service)
      extends RCSubscriptionApiService[Any] {
    import io.scalaland.chimney.dsl._
    private val authenticated = GrpcJwtAuth.authenticated(jwtAuthenticator)

    override def getSubscription(
      request: GetSubscriptionReq): ZIO[Any with Has[RequestContext], Status, GetSubscriptionRes] = {
      for {
        _ <- authenticated
        res <- subscriptionRepo.find(request.id).mapError(toStatus)
      } yield GetSubscriptionRes(res.map(_.transformInto[com.jc.subscription.domain.proto.Subscription]))
    }

    override def createSubscription(
      request: CreateSubscriptionReq): ZIO[Any with Has[RequestContext], Status, CreateSubscriptionRes] = {
      val value = request.transformInto[SubscriptionRepo.Subscription]
      for {
        _ <- authenticated
        _ <- subscriptionRepo.insert(value).mapError(toStatus)
      } yield CreateSubscriptionRes(request.id)
    }

    override def removeSubscription(
      request: RemoveSubscriptionReq): ZIO[Any with Has[RequestContext], Status, RemoveSubscriptionRes] = {
      for {
        _ <- authenticated
        _ <- subscriptionRepo.delete(request.id).mapError(toStatus)
      } yield RemoveSubscriptionRes(request.id)
    }

    override def getSubscriptions(
      request: GetSubscriptionsReq): ZIO[Any with Has[RequestContext], Status, GetSubscriptionsRes] = {
      for {
        _ <- authenticated
        res <- subscriptionRepo.findAll().mapError(toStatus)
      } yield GetSubscriptionsRes(res.map(_.transformInto[com.jc.subscription.domain.proto.Subscription]))
    }
  }

  val live: ZLayer[SubscriptionRepo with JwtAuthenticator, Nothing, SubscriptionGrpcApiHandler] = {
    val res = for {
      jwtAuth <- ZIO.service[JwtAuthenticator.Service]
      repo <- ZIO.service[SubscriptionRepo.Service]
    } yield {
      new LiveSubscriptionApiService(repo, jwtAuth)
    }
    res.toLayer
  }

}
