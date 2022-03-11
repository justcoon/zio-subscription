package com.jc.subscription

import com.jc.cdc.CdcHandler
import com.jc.subscription.domain.proto.{CreateSubscriptionReq, GetSubscriptionReq}
import com.jc.subscription.domain.SubscriptionEntity._
import com.jc.subscription.model.config.{AppCdcConfig, AppConfig}
import com.jc.subscription.module.db.{DbConnection, DbInit}
import com.jc.subscription.module.db.cdc.PostgresCdc
import com.jc.subscription.module.domain.SubscriptionDomain
import com.jc.subscription.module.event.SubscriptionEventProducer
import com.jc.subscription.module.repo.{SubscriptionEventRepo, SubscriptionRepo}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.duration.durationInt
import zio.logging.{Logger, Logging}
import zio.logging.slf4j.Slf4jLogger
import zio.magic._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.{Has, Queue, ZIO, ZLayer}
import zio.config._
import zio.config.syntax._
import zio.config.typesafe._

import java.util.UUID

object SubscriptionCdcSpec extends DefaultRunnableSpec {

  type AppEnvironment = Clock
    with Console with Blocking with Logging with DbConnection with SubscriptionRepo with SubscriptionEventRepo
    with SubscriptionDomain with SubscriptionEventProducer with CdcHandler
    with Has[Queue[SubscriptionEventRepo.SubscriptionEvent]]

  private val testConfig = AppConfig.readConfig[AppCdcConfig](ConfigSource.fromResourcePath.memoize).toLayer

  private val testQueue: ZLayer[Any, Nothing, Has[Queue[SubscriptionEventRepo.SubscriptionEvent]]] =
    Queue.unbounded[SubscriptionEventRepo.SubscriptionEvent].toLayer

  private val layer: ZLayer[Any, TestFailure[Throwable], AppEnvironment] = {
    ZLayer
      .fromMagic[AppEnvironment](
        Clock.live,
        Console.live,
        Blocking.live,
        Slf4jLogger.make((_, message) => message),
        testConfig.narrow(_.db),
        DbConnection.live,
        SubscriptionRepo.live,
        SubscriptionEventRepo.live,
        SubscriptionDomain.live,
        testQueue,
        TestSubscriptionEventProducer.live,
        testConfig.narrow(_.db.connection),
        PostgresCdc.create(SubscriptionEventProducer.processAndSend)
      )
      .mapError(TestFailure.fail)
  }

  override def spec = suite("SubscriptionCdcSpec")(
    testM("create and get") {
      val id = UUID.randomUUID().toString.asSubscriptionId
      for {
        queue <- ZIO.service[Queue[SubscriptionEventRepo.SubscriptionEvent]]
        cr <- SubscriptionDomain.createSubscription(CreateSubscriptionReq(id, "user1".asUserId, "user1@email.com"))
        gr <- SubscriptionDomain.getSubscription(GetSubscriptionReq(id))
        _ <- ZIO.sleep(2.seconds)
        events <- queue.takeAll
      } yield {
        assert(cr.result.isSuccess)(isTrue) && assert(gr.subscription.isDefined)(isTrue) && assert(
          events.exists(_.entityId == id))(isTrue)
      }
    }
  ).provideLayer(layer) @@ beforeAll(DbInit.run.provideLayer(testConfig.narrow(_.db.connection)))
}
