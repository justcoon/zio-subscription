package com.jc.subscription.module.event

import com.jc.subscription.module.repo.SubscriptionEventRepo.SubscriptionEvent
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Has, Task, ZIO, ZLayer}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde

object SubscriptionEventProducer {

  trait Service {
    def send(event: SubscriptionEvent): Task[Unit]
  }

  final class LiveService(topic: String, producer: Producer) extends Service {
    private val layer = ZLayer.succeed(producer)

    override def send(event: SubscriptionEvent): Task[Unit] = {
      for {
        record <- Task.succeed {
          val key = event.entityId
          val value = event.data
          new ProducerRecord(topic, key, value)
        }
        _ <- Producer.produce(record, Serde.string, Serde.byteArray).provideLayer(layer)
      } yield ()
    }
  }

  def create(topic: String): ZLayer[Has[Producer], Nothing, SubscriptionEventProducer] = {
    val res = for {
      producer <- ZIO.service[Producer]
    } yield {
      new LiveService(topic, producer)
    }
    res.toLayer
  }

}
