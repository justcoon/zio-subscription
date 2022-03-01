package com.jc.subscription.module.kafka

import com.jc.subscription.model.config.KafkaConfig
import zio.{Has, ZLayer, ZManaged}
import zio.blocking.Blocking
import zio.kafka.producer.{Producer, ProducerSettings}

object KafkaProducer {

  def create(config: KafkaConfig): ZLayer[Blocking, Throwable, Has[Producer]] = {
    import eu.timepit.refined.auto._
    Producer.make(ProducerSettings(config.addresses)).toLayer
  }

  val live: ZLayer[Has[KafkaConfig] with Blocking, Throwable, Has[Producer]] = {
    import eu.timepit.refined.auto._
    val res = for {
      config <- ZManaged.service[KafkaConfig]
      producer <- Producer.make(ProducerSettings(config.addresses))
    } yield producer
    res.toLayer
  }

}
