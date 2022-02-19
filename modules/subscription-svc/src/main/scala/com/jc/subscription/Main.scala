package com.jc.subscription

import com.jc.subscription.model.config.{AppConfig, PrometheusConfig}
import com.jc.subscription.module.api.{GrpcApiServer, HttpApiServer, SubscriptionGrpcApiHandler}
import com.jc.auth.JwtAuthenticator
import com.jc.cdc.CDCHandler
import com.jc.logging.{LogbackLoggingSystem, LoggingSystem}
import com.jc.logging.api.{LoggingSystemGrpcApi, LoggingSystemGrpcApiHandler}
import com.jc.subscription.module.db.cdc.PostgresCDC
import com.jc.subscription.module.db.{DbConfig, DbConnection}
import com.jc.subscription.module.domain.SubscriptionDomain
import com.jc.subscription.module.event.SubscriptionEventProducer
import com.jc.subscription.module.kafka.KafkaProducer
import com.jc.subscription.module.repo.{SubscriptionEventRepo, SubscriptionRepo}
import io.prometheus.client.exporter.{HTTPServer => PrometheusHttpServer}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.logging.slf4j.Slf4jLogger
import zio.logging.{Logger, Logging}
import zio.metrics.prometheus._
import zio.metrics.prometheus.exporters.Exporters
import zio.metrics.prometheus.helpers._
import scalapb.zio_grpc.{Server => GrpcServer}
import org.http4s.server.{Server => HttpServer}
import eu.timepit.refined.auto._
import zio.magic._

object Main extends App {

  type AppEnvironment = Clock
    with Console with Blocking with JwtAuthenticator with DbConfig with DbConnection with SubscriptionRepo
    with SubscriptionEventRepo with SubscriptionDomain with SubscriptionEventProducer with LoggingSystem
    with LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler with GrpcServer with Has[HttpServer] with Logging
    with Registry with Exporters with CDCHandler

  private def metrics(config: PrometheusConfig): ZIO[AppEnvironment, Throwable, PrometheusHttpServer] = {
    for {
      registry <- getCurrentRegistry()
      _ <- initializeDefaultExports(registry)
      prometheusServer <- http(registry, config.port)
    } yield prometheusServer
  }

  private def handler(events: Chunk[Either[Throwable, SubscriptionEventRepo.SubscriptionEvent]]) = {
    val validEvents = events.collect { case Right(e) => e }

    val errors = events.size - validEvents.size

    for {
      logger <- ZIO.service[Logger[String]]
      producer <- ZIO.service[SubscriptionEventProducer.Service]
      _ <- logger.debug(s"sending events: ${validEvents.mkString(",")}")
      _ <- ZIO.when(errors > 0) {
        logger.warn(s"sending events errors: ${errors}")
      }
      _ <- producer.send(validEvents)
    } yield ()
  }

  private def createAppLayer(appConfig: AppConfig): ZLayer[Any, Throwable, AppEnvironment] = {
    ZLayer.fromMagic[AppEnvironment](
      Clock.live,
      Console.live,
      Blocking.live,
      Slf4jLogger.make((_, message) => message),
      JwtAuthenticator.live(appConfig.jwt),
      DbConfig.create(),
      DbConnection.live,
      SubscriptionRepo.live,
      SubscriptionEventRepo.live,
      SubscriptionDomain.live,
      LogbackLoggingSystem.create(),
      LoggingSystemGrpcApi.live,
      SubscriptionGrpcApiHandler.live,
      KafkaProducer.create(appConfig.kafka),
      SubscriptionEventProducer.create(appConfig.kafka.subscriptionTopic),
      HttpApiServer.create(appConfig.restApi),
      GrpcApiServer.create(appConfig.grpcApi),
      PostgresCDC.create(handler),
      Registry.live,
      Exporters.live
    )
  }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val result: ZIO[zio.ZEnv, Throwable, Nothing] = for {
      appConfig <- AppConfig.getConfig

      runtime: ZIO[AppEnvironment, Throwable, Nothing] = ZIO.runtime[AppEnvironment].flatMap {
        implicit rts: Runtime[AppEnvironment] =>
          metrics(appConfig.prometheus) *>
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
