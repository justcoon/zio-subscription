package com.jc.subscription.module.event

import com.jc.subscription.module.repo.SubscriptionEventRepo.SubscriptionEvent
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Chunk, Has, Task, ZIO, ZLayer}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde

object SubscriptionEventProducer {

  trait Service {
    def send(events: Chunk[SubscriptionEvent]): Task[Unit]
  }

  final class LiveService(topic: String, producer: Producer) extends Service {
    private val layer = ZLayer.succeed(producer)

    override def send(events: Chunk[SubscriptionEvent]): Task[Unit] = {
      val records = events.map { event =>
        val key = event.entityId
        val value = event.data
        new ProducerRecord(topic, key, value)
      }
      Producer.produceChunk(records, Serde.string, Serde.byteArray).provideLayer(layer).unit
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
