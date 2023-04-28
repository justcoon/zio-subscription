package com.jc.subscription.model.config

import zio.Config
import zio.config.refined._

final case class KafkaConfig(addresses: Addresses, subscriptionTopic: TopicName)

object KafkaConfig {

  val kafkaConfig: Config[KafkaConfig] =
    (refineType[Addresses]("addresses") zip refineType[TopicName]("subscription-topic")).map {
      case (addresses, subscriptionTopic) => KafkaConfig(addresses, subscriptionTopic)
    }
}
