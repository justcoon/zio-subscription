package com.jc.subscription.model.config

import pureconfig.generic.semiauto.deriveReader
import zio.config.toKebabCase

final case class KafkaConfig(addresses: Addresses, subscriptionTopic: TopicName)

object KafkaConfig {
  import eu.timepit.refined.pureconfig._
  implicit lazy val configReader = deriveReader[KafkaConfig]

  import zio.config.refined._
  import zio.config.magnolia.descriptor

  implicit val kafkaConfigDescription = descriptor[KafkaConfig].mapKey(toKebabCase)
}
