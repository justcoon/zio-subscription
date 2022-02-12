package com.jc.subscription.module.repo

import com.jc.subscription.domain.SubscriptionEntity.{SubscriptionId, UserId}
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.db.quill.{InstantEncodings, PostgresDbContext, TaggedEncodings}
import io.getquill.Embedded
import zio.{ZIO, ZLayer}

import java.time.Instant

object SubscriptionRepo {

  trait Service[R] extends Repository[R, SubscriptionId, Subscription] {

    def updateAddress(
      id: SubscriptionId,
      address: Option[Address],
      modifiedAt: Option[Instant]): ZIO[R, Throwable, Boolean]
    def updateEmail(id: SubscriptionId, email: String, modifiedAt: Option[Instant]): ZIO[R, Throwable, Boolean]
  }

  final case class Address(
    street: String,
    number: String,
    zip: String,
    city: String,
    state: String,
    country: String
  ) extends Embedded

  object Address {
    import io.circe._
    import io.circe.generic.semiauto._
    implicit val addressDecoder: Decoder[Address] = deriveDecoder[Address]
    implicit val addressEncoder: Encoder[Address] = deriveEncoder[Address]
  }

  final case class Subscription(
    id: SubscriptionId,
    userId: UserId,
    email: String,
    address: Option[Address] = None,
    createdAt: Instant,
    modifiedAt: Option[Instant] = None
  ) extends Repository.Entity[SubscriptionId]

  object Subscription {
    import io.circe._
    import io.circe.generic.semiauto._
    implicit val subscriptionDecoder: Decoder[Subscription] = deriveDecoder[Subscription]
    implicit val subscriptionEncoder: Encoder[Subscription] = deriveEncoder[Subscription]
  }

  final class LiveService() extends Service[DbConnection] with TaggedEncodings with InstantEncodings {
    private val ctx = PostgresDbContext

    import ctx._

    private val query = quote {
      querySchema[Subscription](
        "subscriptions",
        _.address.map(_.street) -> "address_street",
        _.address.map(_.number) -> "address_number",
        _.address.map(_.zip) -> "address_zip",
        _.address.map(_.city) -> "address_city",
        _.address.map(_.state) -> "address_state",
        _.address.map(_.country) -> "address_country"
      )
    }

    override def insert(value: Subscription): ZIO[DbConnection, Throwable, Boolean] = {
      ctx.run(query.insertValue(lift(value))).map(_ > 0)
    }

    override def update(value: Subscription): ZIO[DbConnection, Throwable, Boolean] = {
      ctx.run(query.updateValue(lift(value))).map(_ > 0)
    }

    override def updateAddress(
      id: SubscriptionId,
      address: Option[Address],
      modifiedAt: Option[Instant]): ZIO[DbConnection, Throwable, Boolean] = {
      ctx
        .run(
          query
            .filter(_.id == lift(id))
            .update(
              _.address.map(_.street) -> lift(address.map(_.street)),
              _.address.map(_.number) -> lift(address.map(_.number)),
              _.address.map(_.zip) -> lift(address.map(_.zip)),
              _.address.map(_.city) -> lift(address.map(_.city)),
              _.address.map(_.state) -> lift(address.map(_.state)),
              _.address.map(_.country) -> lift(address.map(_.country)),
              _.modifiedAt -> lift(modifiedAt)
            ))
        .map(_ > 0)
    }

    override def updateEmail(
      id: SubscriptionId,
      email: String,
      modifiedAt: Option[Instant]): ZIO[DbConnection, Throwable, Boolean] = {
      ctx
        .run(query.filter(_.id == lift(id)).update(_.email -> lift(email), _.modifiedAt -> lift(modifiedAt)))
        .map(_ > 0)
    }

    override def delete(id: SubscriptionId): ZIO[DbConnection, Throwable, Boolean] = {
      ctx.run(query.filter(_.id == lift(id)).delete).map(_ > 0)
    }

    override def find(id: SubscriptionId): ZIO[DbConnection, Throwable, Option[Subscription]] = {
      ctx.run(query.filter(_.id == lift(id))).map(_.headOption)
    }

    override def findAll(): ZIO[DbConnection, Throwable, Seq[Subscription]] = {
      ctx.run(query)
    }
  }

  val live: ZLayer[Any, Nothing, SubscriptionRepo] = {
    ZLayer.succeed(new LiveService)
  }

}
