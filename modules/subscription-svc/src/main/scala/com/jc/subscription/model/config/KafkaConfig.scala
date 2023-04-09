package com.jc.subscription.model.config

import zio.config.refined._
import zio.config.magnolia.deriveConfig

final case class KafkaConfig(addresses: Addresses, subscriptionTopic: TopicName)

object KafkaConfig {
  implicit val kafkaConfig = deriveConfig[KafkaConfig]
}
