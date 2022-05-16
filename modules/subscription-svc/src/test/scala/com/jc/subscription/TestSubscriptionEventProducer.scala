package com.jc.subscription

import com.jc.subscription.module.event.SubscriptionEventProducer
import com.jc.subscription.module.repo.SubscriptionEventRepo
import zio.{Chunk, Queue, Task, ZIO, ZLayer}

object TestSubscriptionEventProducer {

  private class TestService(queue: Queue[SubscriptionEventRepo.SubscriptionEvent])
      extends SubscriptionEventProducer.Service {

    override def send(events: Chunk[SubscriptionEventRepo.SubscriptionEvent]): Task[Unit] = {
      ZIO.logDebug(s"sending events: ${events.mkString(",")}") *>
        queue.offerAll(events).unit
    }

    override def processAndSend(
      events: Chunk[Either[Throwable, SubscriptionEventRepo.SubscriptionEvent]]): Task[Unit] = {
      val validEvents = events.collect { case Right(e) => e }

      send(validEvents)
    }
  }

  val live: ZLayer[Queue[SubscriptionEventRepo.SubscriptionEvent], Throwable, SubscriptionEventProducer] = {

    ZLayer.fromZIO(ZIO.service[Queue[SubscriptionEventRepo.SubscriptionEvent]].map(q => new TestService(q)))
  }
}
