package com.jc.subscription.model.config

import pureconfig.generic.semiauto.deriveReader

final case class KafkaConfig(addresses: Addresses, subscriptionTopic: TopicName)

object KafkaConfig {
  import eu.timepit.refined.pureconfig._
  implicit lazy val configReader = deriveReader[KafkaConfig]
}
