package com.jc.subscription

import com.jc.cdc.CdcHandler
import com.jc.subscription.domain.proto.{CreateSubscriptionReq, GetSubscriptionReq}
import com.jc.subscription.domain.SubscriptionEntity._
import com.jc.subscription.model.config.{AppCdcConfig, AppConfig}
import com.jc.subscription.module.db.{DbConnection, DbInit}
import com.jc.subscription.module.db.cdc.PostgresCdc
import com.jc.subscription.module.domain.{LiveSubscriptionDomainService, SubscriptionDomainService}
import com.jc.subscription.module.event.SubscriptionEventProducer
import com.jc.subscription.module.repo.{
  LiveSubscriptionEventRepo,
  LiveSubscriptionRepo,
  SubscriptionEventRepo,
  SubscriptionRepo
}
import zio._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.{Queue, ZIO, ZLayer}
import zio.config._
import zio.config.syntax._
import zio.config.typesafe._
import zio.logging.backend.SLF4J

import java.util.UUID
import zio.test.ZIOSpecDefault

object SubscriptionCdcSpec extends ZIOSpecDefault {

  type AppEnvironment = DbConnection
    with SubscriptionRepo[DbConnection] with SubscriptionEventRepo[DbConnection] with SubscriptionDomainService
    with SubscriptionEventProducer with CdcHandler with Queue[SubscriptionEventRepo.SubscriptionEvent]

  private val testConfig = ZLayer.fromZIO(AppConfig.readConfig[AppCdcConfig](ConfigSource.fromResourcePath.memoize))

  private val testQueue: ZLayer[Any, Nothing, Queue[SubscriptionEventRepo.SubscriptionEvent]] =
    ZLayer.fromZIO(Queue.unbounded[SubscriptionEventRepo.SubscriptionEvent])

  private val layer: ZLayer[Any, Throwable, AppEnvironment] =
    ZLayer.make[AppEnvironment](
      testConfig.narrow(_.db),
      DbConnection.live,
      LiveSubscriptionRepo.layer,
      LiveSubscriptionEventRepo.layer,
      LiveSubscriptionDomainService.layer,
      testQueue,
      TestSubscriptionEventProducer.live,
      testConfig.narrow(_.db.connection),
      PostgresCdc.make(SubscriptionEventProducer.processAndSend)
    ) ++ SLF4J.slf4j(zio.LogLevel.Debug)

  override def spec = suite("SubscriptionCdcSpec")(
    test("create and get") {
      val id = UUID.randomUUID().toString.asSubscriptionId
      for {
        queue <- ZIO.service[Queue[SubscriptionEventRepo.SubscriptionEvent]]
        cr <- SubscriptionDomainService.createSubscription(
          CreateSubscriptionReq(id, "user1".asUserId, "user1@email.com"))
        gr <- SubscriptionDomainService.getSubscription(GetSubscriptionReq(id))
        _ <- ZIO.sleep(2.seconds)
        events <- queue.takeAll
      } yield {
        assert(cr.result.isSuccess)(isTrue) && assert(gr.subscription.isDefined)(isTrue) && assert(
          events.exists(_.entityId == id))(isTrue)
      }
    }
  ).provideLayer(layer) @@ beforeAll(DbInit.run.provideLayer(testConfig.narrow(_.db.connection))) @@ withLiveClock
}
