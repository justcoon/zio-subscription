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
  RemoveSubscriptionRes,
  UpdateSubscriptionAddressReq,
  UpdateSubscriptionAddressRes,
  UpdateSubscriptionEmailReq,
  UpdateSubscriptionEmailRes
}
import com.jc.subscription.domain.proto.ZioSubscriptionApi.RCSubscriptionApiService
import com.jc.subscription.module.domain.SubscriptionDomain
import io.grpc.Status
import scalapb.zio_grpc.RequestContext
import zio.{ZIO, ZLayer}

object SubscriptionGrpcApiHandler {

  def toInternalStatus(e: Throwable): Status = {
    Status.INTERNAL.withDescription(e.getMessage)
  }

  def toStatus(e: scalapb.validate.Failure): Status = {
    Status.INVALID_ARGUMENT.withDescription(getValidationMessage(e))
  }

  def getValidationMessage(e: scalapb.validate.Failure): String =
    new scalapb.validate.FieldValidationException(e).getMessage

  def validated[T](data: T)(implicit
    validator: scalapb.validate.Validator[T]): ZIO[Any, scalapb.validate.Failure, T] = {
    validator.validate(data) match {
      case scalapb.validate.Success => ZIO.succeed(data)
      case e: scalapb.validate.Failure => ZIO.fail(e)
    }
  }

  final class LiveSubscriptionApiService(
    subscriptionService: SubscriptionDomain.Service,
    jwtAuthenticator: JwtAuthenticator.Service)
      extends RCSubscriptionApiService[Any] {

    private val authenticated = GrpcJwtAuth.authenticated(jwtAuthenticator)

    override def getSubscription(
      request: GetSubscriptionReq): ZIO[Any with RequestContext, Status, GetSubscriptionRes] = {
      for {
        _ <- authenticated
        res <- subscriptionService.getSubscription(request).mapError(toInternalStatus)
      } yield res
    }

    override def createSubscription(
      request: CreateSubscriptionReq): ZIO[Any with RequestContext, Status, CreateSubscriptionRes] = {
      for {
        _ <- authenticated
        res <- validated(request).foldZIO(
          e =>
            ZIO.succeed(
              CreateSubscriptionRes(
                request.id,
                CreateSubscriptionRes.Result.Failure(s"Subscription create error (${getValidationMessage(e)})"))),
          _ => subscriptionService.createSubscription(request).mapError(toInternalStatus)
        )
      } yield res
    }

    override def updateSubscriptionEmail(
      request: UpdateSubscriptionEmailReq): ZIO[Any with RequestContext, Status, UpdateSubscriptionEmailRes] = {
      for {
        _ <- authenticated
        res <- validated(request).foldZIO(
          e =>
            ZIO.succeed(
              UpdateSubscriptionEmailRes(
                request.id,
                UpdateSubscriptionEmailRes.Result.Failure(
                  s"Subscription email update error (${getValidationMessage(e)})"))),
          _ => subscriptionService.updateSubscriptionEmail(request).mapError(toInternalStatus)
        )
      } yield res
    }

    override def updateSubscriptionAddress(
      request: UpdateSubscriptionAddressReq): ZIO[Any with RequestContext, Status, UpdateSubscriptionAddressRes] = {
      for {
        _ <- authenticated
        res <- validated(request).foldZIO(
          e =>
            ZIO.succeed(
              UpdateSubscriptionAddressRes(
                request.id,
                UpdateSubscriptionAddressRes.Result.Failure(
                  s"Subscription address update error (${getValidationMessage(e)})"))),
          _ => subscriptionService.updateSubscriptionAddress(request).mapError(toInternalStatus)
        )
      } yield res
    }

    override def removeSubscription(
      request: RemoveSubscriptionReq): ZIO[Any with RequestContext, Status, RemoveSubscriptionRes] = {
      for {
        _ <- authenticated
        res <- subscriptionService.removeSubscription(request).mapError(toInternalStatus)
      } yield res
    }

    override def getSubscriptions(
      request: GetSubscriptionsReq): ZIO[Any with RequestContext, Status, GetSubscriptionsRes] = {
      for {
        _ <- authenticated
        res <- subscriptionService.getSubscriptions(request).mapError(toInternalStatus)
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
    ZLayer.fromZIO(res)
  }

}
