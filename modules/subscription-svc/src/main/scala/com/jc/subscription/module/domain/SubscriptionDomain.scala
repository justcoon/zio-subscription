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
  SubscriptionCreatedPayload,
  SubscriptionPayloadEvent,
  SubscriptionRemovedPayload
}
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.db.quill.PostgresDbContext
import com.jc.subscription.module.repo.{SubscriptionEventRepo, SubscriptionRepo}
import io.getquill.context.zio.ZioJAsyncConnection
import zio.logging.{Logger, Logging}
import zio.{ZIO, ZLayer}

import java.time.Instant

object SubscriptionDomain {

  trait Service {

    def createSubscription(request: com.jc.subscription.domain.proto.CreateSubscriptionReq)
      : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.CreateSubscriptionRes]

    def removeSubscription(request: com.jc.subscription.domain.proto.RemoveSubscriptionReq)
      : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.RemoveSubscriptionRes]

    def getSubscription(request: com.jc.subscription.domain.proto.GetSubscriptionReq)
      : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.GetSubscriptionRes]

    def getSubscriptions(request: com.jc.subscription.domain.proto.GetSubscriptionsReq)
      : zio.ZIO[Any, Throwable, com.jc.subscription.domain.proto.GetSubscriptionsRes]
  }

  final class LiveService(
    subscriptionRepo: SubscriptionRepo.Service[DbConnection],
    subscriptionEventRepo: SubscriptionEventRepo.Service[DbConnection],
    dbConnection: ZioJAsyncConnection,
    logger: Logger[String])
      extends Service {
    import io.scalaland.chimney.dsl._

    private val ctx = PostgresDbContext

    private val dbLayer = ZLayer.succeed(dbConnection)

    private def getEventRecord(event: SubscriptionPayloadEvent) = {
      SubscriptionEventRepo.SubscriptionEvent(
        java.util.UUID.randomUUID().toString,
        event.entityId,
        event.getClass.getName,
        event.toByteArray,
        event.timestamp)
    }

    override def getSubscription(request: GetSubscriptionReq): ZIO[Any, Throwable, GetSubscriptionRes] = {
      logger.info(s"getSubscription - id: ${request.id}") *>
        ctx.transaction {
          subscriptionRepo
            .find(request.id)
            .map(res => GetSubscriptionRes(res.map(_.transformInto[com.jc.subscription.domain.proto.Subscription])))
        }.provideLayer(dbLayer)
    }

    override def createSubscription(request: CreateSubscriptionReq): ZIO[Any, Throwable, CreateSubscriptionRes] = {
      logger.info(s"createSubscription - id: ${request.id}") *>
        ctx.transaction {
          val value = request.into[SubscriptionRepo.Subscription].withFieldConst(_.createdAt, Instant.now()).transform
          val eventData = SubscriptionPayloadEvent(
            value.id,
            value.createdAt,
            SubscriptionPayloadEvent.Payload.Created(
              SubscriptionCreatedPayload(value.userId, value.email, value.address.map(_.transformInto[Address])))
          )
          val event = getEventRecord(eventData)

          for {
            _ <- subscriptionRepo.insert(value)
            _ <- subscriptionEventRepo.insert(event)
          } yield {
            CreateSubscriptionRes(request.id)
          }
        }.provideLayer(dbLayer)
    }

    override def removeSubscription(request: RemoveSubscriptionReq): ZIO[Any, Throwable, RemoveSubscriptionRes] = {
      logger.info(s"removeSubscription - id: ${request.id}") *>
        ctx.transaction {
          val eventData = SubscriptionPayloadEvent(
            request.id,
            Instant.now(),
            SubscriptionPayloadEvent.Payload.Removed(SubscriptionRemovedPayload())
          )
          val event = getEventRecord(eventData)

          for {
            deleted <- subscriptionRepo.delete(request.id)
            _ <- ZIO.when(deleted)(subscriptionEventRepo.insert(event))
          } yield {
            RemoveSubscriptionRes(request.id)
          }
        }.provideLayer(dbLayer)
    }

    override def getSubscriptions(request: GetSubscriptionsReq): ZIO[Any, Throwable, GetSubscriptionsRes] = {
      logger.info("getSubscriptions") *>
        ctx.transaction {
          subscriptionRepo
            .findAll()
            .map(res => GetSubscriptionsRes(res.map(_.transformInto[com.jc.subscription.domain.proto.Subscription])))
        }.provideLayer(dbLayer)
    }
  }

  val live: ZLayer[
    SubscriptionRepo with SubscriptionEventRepo with DbConnection with Logging,
    Nothing,
    SubscriptionDomain] = {
    val res = for {
      logger <- ZIO.service[Logger[String]]
      dbConnection <- ZIO.service[ZioJAsyncConnection]
      subscriptionRepo <- ZIO.service[SubscriptionRepo.Service[DbConnection]]
      subscriptionEventRepo <- ZIO.service[SubscriptionEventRepo.Service[DbConnection]]
    } yield {
      new LiveService(subscriptionRepo, subscriptionEventRepo, dbConnection, logger)
    }
    res.toLayer
  }

  //  def find(id: SubscriptionId): ZIO[SubscriptionDomain, Throwable, Option[Subscription]] = {
  //    ZIO.service[SubscriptionDomain.Service].flatMap(_.find(id))
  //  }
  //
  //  def findAll(): ZIO[SubscriptionDomain, Throwable, Seq[Subscription]] = {
  //    ZIO.service[SubscriptionDomain.Service].flatMap(_.findAll())
  //  }
}
