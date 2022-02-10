package com.jc.subscription.domain

import io.circe.{Decoder, Encoder}
import shapeless.tag
import shapeless.tag.@@

object SubscriptionEntity {
  implicit val subscriptionIdDecoder: Decoder[SubscriptionId] = Decoder[String].map(_.asSubscriptionId)
  implicit val subscriptionIdEncoder: Encoder[SubscriptionId] = Encoder.encodeString.contramap(identity)

  implicit val userIdDecoder: Decoder[UserId] = Decoder[String].map(_.asUserId)
  implicit val userIdEncoder: Encoder[UserId] = Encoder.encodeString.contramap(identity)

  implicit class SubscriptionTaggerOps(v: String) {
    val asSubscriptionId: SubscriptionId = tag[SubscriptionIdTag][String](v)
    val asUserId: UserId = tag[UserIdTag][String](v)

    def as[U]: String @@ U = tag[U][String](v)
  }

  trait SubscriptionIdTag

  type SubscriptionId = String @@ SubscriptionIdTag

  trait UserIdTag

  type UserId = String @@ UserIdTag

  trait SubscriptionEvent {
    def entityId: SubscriptionId
    def timestamp: java.time.Instant
  }

  val defaultPartitionKeyStrategy: SubscriptionEvent => String = (e: SubscriptionEvent) => s"${e.entityId}"
}
