package com.jc.subscription.module.repo

import com.jc.subscription.domain.SubscriptionEntity.{SubscriptionId, UserId}
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.db.quill.{InstantEncodings, PostgresDbContext, TaggedEncodings}
import io.getquill.Embedded
import zio.{ZIO, ZLayer}

import java.time.Instant

object SubscriptionRepo {

  trait Service[R] extends Repository[R, SubscriptionId, Subscription]

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
    import shapeless._

    val emailLens: Lens[Subscription, String] = lens[Subscription].email
    val addressLens: Lens[Subscription, Option[Address]] = lens[Subscription].address

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
      ctx.run(query.insertValue(lift(value))).map(_ => true)
    }

    override def update(value: Subscription): ZIO[DbConnection, Throwable, Boolean] = {
      ctx.run(query.updateValue(lift(value))).map(_ => true)
    }

    override def delete(id: SubscriptionId): ZIO[DbConnection, Throwable, Boolean] = {
      ctx.run(query.filter(_.id == lift(id)).delete).map(_ => true)
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
