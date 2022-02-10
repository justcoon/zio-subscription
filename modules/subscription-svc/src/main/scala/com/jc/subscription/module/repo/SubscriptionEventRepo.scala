package com.jc.subscription.module.repo

import com.jc.subscription.domain.SubscriptionEntity.SubscriptionId
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.db.quill.{InstantEncodings, PostgresDbContext, TaggedEncodings}
import zio.{ZIO, ZLayer}

import java.time.Instant
import java.util.Base64

object SubscriptionEventRepo {

  trait Service[R] {
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

    val cdcDecoder: Decoder[SubscriptionEventRepo.SubscriptionEvent] = (c: HCursor) =>
      for {
        id <- c.downField("id").as[String]
        entityId <- c.downField("entity_id").as[SubscriptionId]
        tpe <- c.downField("type").as[String]
        d <- c.downField("data").as[String]
        at <- c.downField("created_at").as[Long]
      } yield {
        val data = Base64.getDecoder.decode(d)
        val createdAt = Instant.ofEpochMilli(at / 1000)
        SubscriptionEventRepo.SubscriptionEvent(id, entityId, tpe, data, createdAt)
      }
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
  }

  val live: ZLayer[Any, Nothing, SubscriptionEventRepo] = {
    ZLayer.succeed(new LiveService)
  }

}
