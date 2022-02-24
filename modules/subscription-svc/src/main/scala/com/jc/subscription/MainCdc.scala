package com.jc.subscription

import com.jc.cdc.CDCHandler
import com.jc.subscription.model.config.AppConfig
import com.jc.subscription.module.api.HttpApiServer
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.db.cdc.PostgresCDC
import com.jc.subscription.module.event.SubscriptionEventProducer
import com.jc.subscription.module.kafka.KafkaProducer
import com.jc.subscription.module.metrics.PrometheusMetricsExporter
import eu.timepit.refined.auto._
import org.http4s.server.{Server => HttpServer}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger
import zio.magic._
import zio.metrics.prometheus._
import zio.metrics.prometheus.exporters.Exporters

object MainCdc extends App {

  type AppEnvironment = Clock
    with Console with Blocking with DbConnection with SubscriptionEventProducer
    with Has[HttpServer] with Logging with Registry with Exporters
    with CDCHandler

  private def createAppLayer(appConfig: AppConfig): ZLayer[Any, Throwable, AppEnvironment] = {
    ZLayer.fromMagic[AppEnvironment](
      Clock.live,
      Console.live,
      Blocking.live,
      Slf4jLogger.make((_, message) => message),
      DbConnection.create(appConfig.db.connection),
      KafkaProducer.create(appConfig.kafka),
      SubscriptionEventProducer.create(appConfig.kafka.subscriptionTopic),
      HttpApiServer.create(appConfig.restApi),
      PostgresCDC.create(appConfig.db, SubscriptionEventProducer.processAndSend),
      Registry.live,
      Exporters.live
    )
  }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val result: ZIO[zio.ZEnv, Throwable, Nothing] = for {
      appConfig <- AppConfig.getConfig

      runtime: ZIO[AppEnvironment, Throwable, Nothing] = ZIO.runtime[AppEnvironment].flatMap {
        implicit rts: Runtime[AppEnvironment] =>
          PrometheusMetricsExporter.create(appConfig.prometheus) *>
            ZIO.never
      }

      appLayer = createAppLayer(appConfig)

      program <- runtime.provideCustomLayer[Throwable, AppEnvironment](appLayer)
    } yield program

    result
      .foldM(
        failure = err => {
          ZIO.accessM[ZEnv](_.get[Console.Service].putStrLn(s"Execution failed with: $err")).ignore *> ZIO.succeed(
            ExitCode.failure)
        },
        success = _ => ZIO.succeed(ExitCode.success)
      )
  }
}
