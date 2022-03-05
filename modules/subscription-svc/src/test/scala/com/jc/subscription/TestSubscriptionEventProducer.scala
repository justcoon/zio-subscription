package com.jc.subscription

import com.jc.subscription.module.event.SubscriptionEventProducer
import com.jc.subscription.module.repo.SubscriptionEventRepo
import zio.{Chunk, Has, Queue, Task, ZIO, ZLayer}
import zio.logging.{Logger, Logging}

object TestSubscriptionEventProducer {

  private class TestService(queue: Queue[SubscriptionEventRepo.SubscriptionEvent], logger: Logger[String])
      extends SubscriptionEventProducer.Service {

    override def send(events: Chunk[SubscriptionEventRepo.SubscriptionEvent]): Task[Unit] = {
      logger.debug(s"sending events: ${events.mkString(",")}") *>
        queue.offerAll(events).unit
    }

    override def processAndSend(
      events: Chunk[Either[Throwable, SubscriptionEventRepo.SubscriptionEvent]]): Task[Unit] = {
      val validEvents = events.collect { case Right(e) => e }

      send(validEvents)
    }
  }

  val live
    : ZLayer[Has[Queue[SubscriptionEventRepo.SubscriptionEvent]] with Logging, Throwable, SubscriptionEventProducer] = {
    val res = for {
      logger <- ZIO.service[Logger[String]]
      queue <- ZIO.service[Queue[SubscriptionEventRepo.SubscriptionEvent]]
    } yield new TestService(queue, logger)

    res.toLayer
  }
}
