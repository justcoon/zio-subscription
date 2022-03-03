package com.jc.subscription.model.config

import zio.config.toKebabCase
import zio.config.refined._
import zio.config.magnolia.descriptor

final case class KafkaConfig(addresses: Addresses, subscriptionTopic: TopicName)

object KafkaConfig {
  implicit val kafkaConfigDescription = descriptor[KafkaConfig].mapKey(toKebabCase)
}
