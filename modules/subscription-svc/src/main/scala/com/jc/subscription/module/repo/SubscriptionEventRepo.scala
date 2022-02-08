package com.jc.subscription.module.repo

import com.jc.subscription.domain.SubscriptionEntity.SubscriptionId
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.db.quill.{InstantEncodings, PostgresDbContext, TaggedEncodings}
import zio.{ZIO, ZLayer}

import java.time.Instant

object SubscriptionEventRepo {

  trait Service[R] /*extends Repository[R, String, SubscriptionEvent]*/ {
    def insert(value: SubscriptionEvent): ZIO[R, Throwable, Boolean]
  }

  final case class SubscriptionEvent(
    id: String,
    entityId: SubscriptionId,
    `type`: String,
    data: Array[Byte],
    createdAt: Instant
  ) extends Repository.Entity[String]

  object SubscriptionEvent {
    import io.circe._
    import io.circe.generic.semiauto._
    implicit val subscriptionDecoder: Decoder[SubscriptionEvent] = deriveDecoder[SubscriptionEvent]
    implicit val subscriptionEncoder: Encoder[SubscriptionEvent] = deriveEncoder[SubscriptionEvent]
  }

  final class LiveService() extends Service[DbConnection] with TaggedEncodings with InstantEncodings {
    private val ctx = PostgresDbContext

    import ctx._

    private val query = quote {
      querySchema[SubscriptionEvent]("subscription_events")
    }

    override def insert(value: SubscriptionEvent): ZIO[DbConnection, Throwable, Boolean] = {
      ctx.run(query.insertValue(lift(value))).map(_ => true)
    }

//    override def update(value: SubscriptionEvent): ZIO[DbConnection, Throwable, Boolean] = {
//      ctx.run(query.updateValue(lift(value))).map(_ => true)
//    }
//
//    override def delete(id: String): ZIO[DbConnection, Throwable, Boolean] = {
//      ctx.run(query.filter(_.id == lift(id)).delete).map(_ => true)
//    }
//
//    override def find(id: String): ZIO[DbConnection, Throwable, Option[SubscriptionEvent]] = {
//      ctx.run(query.filter(_.id == lift(id))).map(_.headOption)
//    }
//
//    override def findAll(): ZIO[DbConnection, Throwable, Seq[SubscriptionEvent]] = {
//      ctx.run(query)
//    }
  }

  val live: ZLayer[Any, Nothing, SubscriptionEventRepo] = {
    ZLayer.succeed(new LiveService)
  }

}
