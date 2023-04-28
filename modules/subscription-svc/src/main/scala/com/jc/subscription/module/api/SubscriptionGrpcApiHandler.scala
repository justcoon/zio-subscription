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
import com.jc.subscription.module.domain.SubscriptionDomainService
import io.grpc.{Status, StatusException}
import scalapb.zio_grpc.RequestContext
import zio.{ZIO, ZLayer}

final class SubscriptionGrpcApiHandler(
  subscriptionService: SubscriptionDomainService,
  jwtAuthenticator: JwtAuthenticator)
    extends RCSubscriptionApiService {

  private val authenticated = GrpcJwtAuth.authenticated(jwtAuthenticator)

  override def getSubscription(
    request: GetSubscriptionReq,
    context: RequestContext): ZIO[Any, StatusException, GetSubscriptionRes] = {
    for {
      _ <- authenticated(context)
      res <- subscriptionService.getSubscription(request).mapError(SubscriptionGrpcApiHandler.toInternalStatusException)
    } yield res
  }

  override def createSubscription(
    request: CreateSubscriptionReq,
    context: RequestContext): ZIO[Any, StatusException, CreateSubscriptionRes] = {
    for {
      _ <- authenticated(context)
      res <- SubscriptionGrpcApiHandler
        .validated(request)
        .foldZIO(
          e =>
            ZIO.succeed(
              CreateSubscriptionRes(
                request.id,
                CreateSubscriptionRes.Result.Failure(
                  s"Subscription create error (${SubscriptionGrpcApiHandler.getValidationMessage(e)})"))),
          _ =>
            subscriptionService
              .createSubscription(request)
              .mapError(SubscriptionGrpcApiHandler.toInternalStatusException)
        )
    } yield res
  }

  override def updateSubscriptionEmail(
    request: UpdateSubscriptionEmailReq,
    context: RequestContext): ZIO[Any, StatusException, UpdateSubscriptionEmailRes] = {
    for {
      _ <- authenticated(context)
      res <- SubscriptionGrpcApiHandler
        .validated(request)
        .foldZIO(
          e =>
            ZIO.succeed(
              UpdateSubscriptionEmailRes(
                request.id,
                UpdateSubscriptionEmailRes.Result.Failure(
                  s"Subscription email update error (${SubscriptionGrpcApiHandler.getValidationMessage(e)})"))),
          _ =>
            subscriptionService
              .updateSubscriptionEmail(request)
              .mapError(SubscriptionGrpcApiHandler.toInternalStatusException)
        )
    } yield res
  }

  override def updateSubscriptionAddress(
    request: UpdateSubscriptionAddressReq,
    context: RequestContext): ZIO[Any, StatusException, UpdateSubscriptionAddressRes] = {
    for {
      _ <- authenticated(context)
      res <- SubscriptionGrpcApiHandler
        .validated(request)
        .foldZIO(
          e =>
            ZIO.succeed(
              UpdateSubscriptionAddressRes(
                request.id,
                UpdateSubscriptionAddressRes.Result.Failure(
                  s"Subscription address update error (${SubscriptionGrpcApiHandler.getValidationMessage(e)})"))),
          _ =>
            subscriptionService
              .updateSubscriptionAddress(request)
              .mapError(SubscriptionGrpcApiHandler.toInternalStatusException)
        )
    } yield res
  }

  override def removeSubscription(
    request: RemoveSubscriptionReq,
    context: RequestContext): ZIO[Any, StatusException, RemoveSubscriptionRes] = {
    for {
      _ <- authenticated(context)
      res <- subscriptionService
        .removeSubscription(request)
        .mapError(SubscriptionGrpcApiHandler.toInternalStatusException)
    } yield res
  }

  override def getSubscriptions(
    request: GetSubscriptionsReq,
    context: RequestContext): ZIO[Any, StatusException, GetSubscriptionsRes] = {
    for {
      _ <- authenticated(context)
      res <- subscriptionService
        .getSubscriptions(request)
        .mapError(SubscriptionGrpcApiHandler.toInternalStatusException)
    } yield res
  }
}

object SubscriptionGrpcApiHandler {

  def toInternalStatusException(e: Throwable): StatusException = {
    new StatusException(Status.INTERNAL.withDescription(e.getMessage))
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

  val layer: ZLayer[SubscriptionDomainService with JwtAuthenticator, Nothing, SubscriptionGrpcApiHandler] = {
    ZLayer.fromFunction(new SubscriptionGrpcApiHandler(_, _))
  }

}
