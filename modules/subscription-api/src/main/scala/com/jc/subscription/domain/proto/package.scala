package com.jc.subscription.domain

import com.google.protobuf.timestamp.Timestamp
import com.jc.subscription.domain.SubscriptionEntity._
import scalapb.TypeMapper

import java.time.Instant

package object proto {

  implicit val userIdTypeMapper: TypeMapper[String, UserId] = TypeMapper[String, UserId](_.asUserId)(identity)

  implicit val subscriptionIdTypeMapper: TypeMapper[String, SubscriptionId] =
    TypeMapper[String, SubscriptionId](_.asSubscriptionId)(identity)

  implicit val instantTypeMapper: TypeMapper[Timestamp, Instant] = TypeMapper[Timestamp, Instant] { timestamp =>
    Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong)
  } { instant => Timestamp.of(instant.getEpochSecond, instant.getNano) }
}
