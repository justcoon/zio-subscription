package com.jc.subscription.module.event

import com.jc.subscription.module.repo.SubscriptionEventRepo.SubscriptionEvent
import org.apache.kafka.clients.producer.ProducerRecord
import zio.{Chunk, Has, Task, ZIO, ZLayer}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.logging.{Logger, Logging}

object SubscriptionEventProducer {

  trait Service {
    def send(events: Chunk[SubscriptionEvent]): Task[Unit]
    def processAndSend(events: Chunk[Either[Throwable, SubscriptionEvent]]): Task[Unit]
  }

  final class LiveService(topic: String, producer: Producer, logger: Logger[String]) extends Service {
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
        _ <- logger.debug(s"sending events: ${validEvents.mkString(",")}")
        _ <- sendInternal(validEvents)
        _ <- logger.warn(s"sending events errors: ${errors}").when(errors > 0)
      } yield ()
    }

    override def send(events: Chunk[SubscriptionEvent]): Task[Unit] = {
      logger.debug(s"sending events: ${events.mkString(",")}") *> sendInternal(events)
    }
  }

  def create(topic: String): ZLayer[Has[Producer] with Logging, Nothing, SubscriptionEventProducer] = {
    val res = for {
      logger <- ZIO.service[Logger[String]]
      producer <- ZIO.service[Producer]
    } yield {
      new LiveService(topic, producer, logger)
    }
    res.toLayer
  }

  def send(events: Chunk[SubscriptionEvent]): ZIO[SubscriptionEventProducer, Throwable, Unit] = {
    ZIO.service[SubscriptionEventProducer.Service].flatMap(_.send(events))
  }

  def processAndSend(
    events: Chunk[Either[Throwable, SubscriptionEvent]]): ZIO[SubscriptionEventProducer, Throwable, Unit] = {
    ZIO.service[SubscriptionEventProducer.Service].flatMap(_.processAndSend(events))
  }

}
