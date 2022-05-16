package com.jc.subscription.module.event

import com.jc.subscription.module.repo.SubscriptionEventRepo.SubscriptionEvent
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Chunk, Task, ZIO, ZLayer}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde

trait SubscriptionEventProducer {
  def send(events: Chunk[SubscriptionEvent]): Task[Unit]
  def processAndSend(events: Chunk[Either[Throwable, SubscriptionEvent]]): Task[Unit]
}

final class LiveSubscriptionEventProducer(topic: String, producer: Producer) extends SubscriptionEventProducer {
  private val layer = ZLayer.succeed(producer)

  private def sendInternal(events: Chunk[SubscriptionEvent]): Task[Unit] = {
    val records = events.map { event =>
      val key = event.entityId
      val value = event.data
      new ProducerRecord(topic, key, value)
    }
    Producer.produceChunk(records, Serde.string, Serde.byteArray).provideLayer(layer).unit
  }

  override def processAndSend(events: Chunk[Either[Throwable, SubscriptionEvent]]): Task[Unit] = {
    val validEvents = events.collect { case Right(e) => e }
    val errors = events.size - validEvents.size
    for {
      _ <- ZIO.logDebug(s"sending events: ${validEvents.mkString(",")}")
      _ <- sendInternal(validEvents)
      _ <- ZIO.logWarning(s"sending events errors: ${errors}").when(errors > 0)
    } yield ()
  }

  override def send(events: Chunk[SubscriptionEvent]): Task[Unit] = {
    ZIO.logDebug(s"sending events: ${events.mkString(",")}") *> sendInternal(events)
  }
}

object LiveSubscriptionEventProducer {

  def make(topic: String): ZLayer[Producer, Nothing, SubscriptionEventProducer] = {
    ZLayer.fromZIO(ZIO.service[Producer].map(new LiveSubscriptionEventProducer(topic, _)))
  }

}

object SubscriptionEventProducer {

  def send(events: Chunk[SubscriptionEvent]): ZIO[SubscriptionEventProducer, Throwable, Unit] = {
    ZIO.service[SubscriptionEventProducer].flatMap(_.send(events))
  }

  def processAndSend(
    events: Chunk[Either[Throwable, SubscriptionEvent]]): ZIO[SubscriptionEventProducer, Throwable, Unit] = {
    ZIO.service[SubscriptionEventProducer].flatMap(_.processAndSend(events))
  }
}
