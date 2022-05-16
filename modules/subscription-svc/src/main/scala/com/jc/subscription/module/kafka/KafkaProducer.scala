package com.jc.subscription.module.kafka

import com.jc.subscription.model.config.KafkaConfig
import zio.{ZIO, ZLayer}
import zio.kafka.producer.{Producer, ProducerSettings}

object KafkaProducer {

  def create(config: KafkaConfig): ZLayer[Any, Throwable, Producer] = {
    import eu.timepit.refined.auto._
    ZLayer.scoped {
      Producer.make(ProducerSettings(config.addresses))
    }
  }

  val live: ZLayer[KafkaConfig, Throwable, Producer] = {
    import eu.timepit.refined.auto._
    ZLayer.scoped {
      for {
        config <- ZIO.service[KafkaConfig]
        producer <- Producer.make(ProducerSettings(config.addresses))
      } yield producer
    }
  }

}
