package com.jc.subscription.module.repo

import com.jc.subscription.domain.SubscriptionEntity.{SubscriptionId, UserId}
import com.jc.subscription.module.repo.quill.{PostgresDBContext, TaggedEncodings}
import io.getquill.{Embedded, SnakeCase}
import io.getquill.context.zio.{PostgresZioJAsyncContext, ZioJAsyncConnection}
import zio.logging.{Logger, Logging}
import zio.{Has, ZIO, ZLayer}

object SubscriptionRepo {

  trait Service extends Repository[Any, SubscriptionId, Subscription]

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
    address: Option[Address] = None
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

  final class LiveService(dbConnection: ZioJAsyncConnection) extends Service with TaggedEncodings {
    private val ctx = PostgresDBContext

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

    private val dbLayer = ZLayer.succeed(dbConnection)

    override def insert(value: Subscription): ZIO[Any, Throwable, Boolean] = {
      ctx.transaction {
        ctx.run(query.insertValue(lift(value))).map(_ => true)
      }.provideLayer(dbLayer)
    }

    override def update(value: Subscription): ZIO[Any, Throwable, Boolean] = {
      ctx.transaction {
        ctx.run(query.updateValue(lift(value))).map(_ => true)
      }.provideLayer(dbLayer)
    }

    override def delete(id: SubscriptionId): ZIO[Any, Throwable, Boolean] = {
      ctx.transaction {
        ctx.run(query.filter(_.id == lift(id)).delete).map(_ => true)
      }.provideLayer(dbLayer)
    }

    override def find(id: SubscriptionId): ZIO[Any, Throwable, Option[Subscription]] = {
      ctx.transaction {
        ctx.run(query.filter(_.id == lift(id))).map(_.headOption)
      }.provideLayer(dbLayer)
    }

    override def findAll(): ZIO[Any, Throwable, Seq[Subscription]] = {
      ctx.transaction {
        ctx.run(query)
      }.provideLayer(dbLayer)
    }
  }

  def find(id: SubscriptionId): ZIO[Has[SubscriptionRepo.Service], Throwable, Option[Subscription]] = {
    ZIO.service[SubscriptionRepo.Service].flatMap(_.find(id))
  }

  def findAll(): ZIO[Has[SubscriptionRepo.Service], Throwable, Seq[Subscription]] = {
    ZIO.service[SubscriptionRepo.Service].flatMap(_.findAll())
  }
}
