package com.jc.subscription.model.config

import pureconfig.generic.semiauto.deriveReader
import zio.config.toKebabCase
import zio.config.refined._
import zio.config.magnolia.descriptor

final case class KafkaConfig(addresses: Addresses, subscriptionTopic: TopicName)

object KafkaConfig {
  import eu.timepit.refined.pureconfig._
  implicit lazy val configReader = deriveReader[KafkaConfig]

  implicit val kafkaConfigDescription = descriptor[KafkaConfig].mapKey(toKebabCase)
}
