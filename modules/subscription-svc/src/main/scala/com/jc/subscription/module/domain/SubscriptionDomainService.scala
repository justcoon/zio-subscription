package com.jc.subscription.module.domain

import com.jc.subscription.domain.proto.{
  Address,
  CreateSubscriptionReq,
  CreateSubscriptionRes,
  GetSubscriptionReq,
  GetSubscriptionRes,
  GetSubscriptionsReq,
  GetSubscriptionsRes,
  RemoveSubscriptionReq,
  RemoveSubscriptionRes,
  SubscriptionAddressUpdatedPayload,
  SubscriptionCreatedPayload,
  SubscriptionEmailUpdatedPayload,
  SubscriptionPayloadEvent,
  SubscriptionRemovedPayload,
  UpdateSubscriptionAddressReq,
  UpdateSubscriptionAddressRes,
  UpdateSubscriptionEmailReq,
  UpdateSubscriptionEmailRes
}
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.db.quill.PostgresDbContext
import com.jc.subscription.module.repo.{SubscriptionEventRepo, SubscriptionRepo}
import io.getquill.context.zio.ZioJAsyncConnection
import zio.{ZIO, ZLayer}

import java.time.Instant

trait SubscriptionDomainService {

  def createSubscription(request: com.jc.subscription.domain.proto.CreateSubscriptionReq)
    : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.CreateSubscriptionRes]

  def updateSubscriptionAddress(request: com.jc.subscription.domain.proto.UpdateSubscriptionAddressReq)
    : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.UpdateSubscriptionAddressRes]

  def updateSubscriptionEmail(request: com.jc.subscription.domain.proto.UpdateSubscriptionEmailReq)
    : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.UpdateSubscriptionEmailRes]

  def removeSubscription(request: com.jc.subscription.domain.proto.RemoveSubscriptionReq)
    : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.RemoveSubscriptionRes]

  def getSubscription(request: com.jc.subscription.domain.proto.GetSubscriptionReq)
    : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.GetSubscriptionRes]

  def getSubscriptions(request: com.jc.subscription.domain.proto.GetSubscriptionsReq)
    : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.GetSubscriptionsRes]
}

object SubscriptionDomainService {

  def createSubscription(request: com.jc.subscription.domain.proto.CreateSubscriptionReq)
    : zio.ZIO[SubscriptionDomainService, Throwable, com.jc.subscription.domain.proto.CreateSubscriptionRes] =
    ZIO.serviceWithZIO[SubscriptionDomainService](_.createSubscription(request))

  def updateSubscriptionAddress(request: com.jc.subscription.domain.proto.UpdateSubscriptionAddressReq)
    : zio.ZIO[SubscriptionDomainService, Throwable, com.jc.subscription.domain.proto.UpdateSubscriptionAddressRes] =
    ZIO.serviceWithZIO[SubscriptionDomainService](_.updateSubscriptionAddress(request))

  def updateSubscriptionEmail(request: com.jc.subscription.domain.proto.UpdateSubscriptionEmailReq)
    : zio.ZIO[SubscriptionDomainService, Throwable, com.jc.subscription.domain.proto.UpdateSubscriptionEmailRes] =
    ZIO.serviceWithZIO[SubscriptionDomainService](_.updateSubscriptionEmail(request))

  def removeSubscription(request: com.jc.subscription.domain.proto.RemoveSubscriptionReq)
    : zio.ZIO[SubscriptionDomainService, Throwable, com.jc.subscription.domain.proto.RemoveSubscriptionRes] =
    ZIO.serviceWithZIO[SubscriptionDomainService](_.removeSubscription(request))

  def getSubscription(request: com.jc.subscription.domain.proto.GetSubscriptionReq)
    : zio.ZIO[SubscriptionDomainService, Throwable, com.jc.subscription.domain.proto.GetSubscriptionRes] =
    ZIO.serviceWithZIO[SubscriptionDomainService](_.getSubscription(request))

  def getSubscriptions(request: com.jc.subscription.domain.proto.GetSubscriptionsReq)
    : zio.ZIO[SubscriptionDomainService, Throwable, com.jc.subscription.domain.proto.GetSubscriptionsRes] =
    ZIO.serviceWithZIO[SubscriptionDomainService](_.getSubscriptions(request))
}

final class LiveSubscriptionDomainService(
  subscriptionRepo: SubscriptionRepo[DbConnection],
  subscriptionEventRepo: SubscriptionEventRepo[DbConnection],
  dbConnection: ZioJAsyncConnection)
    extends SubscriptionDomainService {
  import io.scalaland.chimney.dsl._

  private val ctx = PostgresDbContext

  private val dbLayer = ZLayer.succeed(dbConnection)

  private def getEventRecord(event: SubscriptionPayloadEvent) = {
    SubscriptionEventRepo.SubscriptionEvent(
      java.util.UUID.randomUUID().toString,
      event.entityId,
      event.getClass.getName,
      event.payload.getClass.getName,
      event.toByteArray,
      event.timestamp)
  }

  override def getSubscription(request: GetSubscriptionReq): ZIO[Any, Throwable, GetSubscriptionRes] = {
    ZIO.logInfo(s"getSubscription - id: ${request.id}") *>
      ctx.transaction {
        subscriptionRepo
          .find(request.id)
          .map(res => GetSubscriptionRes(res.map(_.transformInto[com.jc.subscription.domain.proto.Subscription])))
      }.provideLayer(dbLayer)
  }

  override def createSubscription(request: CreateSubscriptionReq): ZIO[Any, Throwable, CreateSubscriptionRes] = {
    ZIO.logInfo(s"createSubscription - id: ${request.id}") *>
      ctx.transaction {
        val value = request.into[SubscriptionRepo.Subscription].withFieldConst(_.createdAt, Instant.now()).transform

        for {
          stored <- subscriptionRepo.find(value.id)
          _ <- ZIO.when(stored.isEmpty) {
            subscriptionRepo.insert(value)
          }
          _ <- ZIO.when(stored.isEmpty) {
            val event = SubscriptionPayloadEvent(
              value.id,
              value.createdAt,
              SubscriptionPayloadEvent.Payload.Created(
                SubscriptionCreatedPayload(value.userId, value.email, value.address.map(_.transformInto[Address])))
            )
            val eventRecord = getEventRecord(event)
            subscriptionEventRepo.insert(eventRecord)
          }
        } yield {
          val result = if (stored.isEmpty) {
            CreateSubscriptionRes.Result.Success("Subscription created")
          } else CreateSubscriptionRes.Result.Failure("Subscription already exist")
          CreateSubscriptionRes(request.id, result)
        }
      }.provideLayer(dbLayer)
  }

  override def updateSubscriptionAddress(
    request: UpdateSubscriptionAddressReq): ZIO[Any, Throwable, UpdateSubscriptionAddressRes] = {
    ZIO.logInfo(s"updateSubscriptionAddress - id: ${request.id}") *>
      ctx.transaction {
        val value = request.address.map(_.transformInto[SubscriptionRepo.Address])
        val at = Instant.now()
        for {
          updated <- subscriptionRepo.updateAddress(request.id, value, Some(at))
          _ <- ZIO.when(updated) {
            val event = SubscriptionPayloadEvent(
              request.id,
              at,
              SubscriptionPayloadEvent.Payload.AddressUpdated(SubscriptionAddressUpdatedPayload(request.address))
            )
            val eventRecord = getEventRecord(event)
            subscriptionEventRepo.insert(eventRecord)
          }
        } yield {
          val result = if (updated) {
            UpdateSubscriptionAddressRes.Result.Success("Subscription address updated")
          } else UpdateSubscriptionAddressRes.Result.Failure("Subscription not found")
          UpdateSubscriptionAddressRes(request.id, result)
        }
      }.provideLayer(dbLayer)
  }

  override def updateSubscriptionEmail(
    request: UpdateSubscriptionEmailReq): ZIO[Any, Throwable, UpdateSubscriptionEmailRes] = {
    ZIO.logInfo(s"updateSubscriptionEmail - id: ${request.id}") *>
      ctx.transaction {
        val at = Instant.now()
        for {
          updated <- subscriptionRepo.updateEmail(request.id, request.email, Some(at))
          _ <- ZIO.when(updated) {
            val eventRecord = SubscriptionPayloadEvent(
              request.id,
              at,
              SubscriptionPayloadEvent.Payload.EmailUpdated(SubscriptionEmailUpdatedPayload(request.email))
            )
            val event = getEventRecord(eventRecord)
            subscriptionEventRepo.insert(event)
          }
        } yield {
          val result = if (updated) {
            UpdateSubscriptionEmailRes.Result.Success("Subscription email updated")
          } else UpdateSubscriptionEmailRes.Result.Failure("Subscription not found")
          UpdateSubscriptionEmailRes(request.id, result)
        }
      }.provideLayer(dbLayer)
  }

  override def removeSubscription(request: RemoveSubscriptionReq): ZIO[Any, Throwable, RemoveSubscriptionRes] = {
    ZIO.logInfo(s"removeSubscription - id: ${request.id}") *>
      ctx.transaction {
        for {
          deleted <- subscriptionRepo.delete(request.id)
          _ <- ZIO.when(deleted) {
            val event = SubscriptionPayloadEvent(
              request.id,
              Instant.now(),
              SubscriptionPayloadEvent.Payload.Removed(SubscriptionRemovedPayload())
            )
            val eventRecord = getEventRecord(event)
            subscriptionEventRepo.insert(eventRecord)
          }
        } yield {
          val result = if (deleted) {
            RemoveSubscriptionRes.Result.Success("Subscription deleted")
          } else RemoveSubscriptionRes.Result.Failure("Subscription not found")
          RemoveSubscriptionRes(request.id, result)
        }
      }.provideLayer(dbLayer)
  }

  override def getSubscriptions(request: GetSubscriptionsReq): ZIO[Any, Throwable, GetSubscriptionsRes] = {
    ZIO.logInfo("getSubscriptions") *>
      ctx.transaction {
        subscriptionRepo
          .findAll()
          .map(res => GetSubscriptionsRes(res.map(_.transformInto[com.jc.subscription.domain.proto.Subscription])))
      }.provideLayer(dbLayer)
  }
}

object LiveSubscriptionDomainService {

  val layer: ZLayer[
    SubscriptionRepo[DbConnection] with SubscriptionEventRepo[DbConnection] with DbConnection,
    Nothing,
    SubscriptionDomainService] = {
    val res = for {
      dbConnection <- ZIO.service[ZioJAsyncConnection]
      subscriptionRepo <- ZIO.service[SubscriptionRepo[DbConnection]]
      subscriptionEventRepo <- ZIO.service[SubscriptionEventRepo[DbConnection]]
    } yield {
      new LiveSubscriptionDomainService(subscriptionRepo, subscriptionEventRepo, dbConnection)
    }
    ZLayer.fromZIO(res)
  }

}
