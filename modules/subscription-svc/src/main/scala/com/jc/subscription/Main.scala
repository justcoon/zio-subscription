package com.jc.subscription

import com.jc.subscription.model.config.{AppAllConfig, AppCdcConfig, AppConfig, AppMode, AppSvcConfig}
import com.jc.subscription.module.api.{GrpcApiServer, HttpApiServer, SubscriptionGrpcApiHandler}
import com.jc.auth.JwtAuthenticator
import com.jc.cdc.CdcHandler
import com.jc.logging.{LogbackLoggingSystem, LoggingSystem}
import com.jc.logging.api.{LoggingSystemGrpcApi, LoggingSystemGrpcApiHandler}
import com.jc.subscription.module.db.cdc.PostgresCdc
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.domain.SubscriptionDomain
import com.jc.subscription.module.event.SubscriptionEventProducer
import com.jc.subscription.module.kafka.KafkaProducer
import com.jc.subscription.module.metrics.PrometheusMetricsExporter
import com.jc.subscription.module.repo.{SubscriptionEventRepo, SubscriptionRepo}
import com.typesafe.config.ConfigFactory
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.logging.slf4j.Slf4jLogger
import zio.logging.{Logger, Logging}
import zio.metrics.prometheus._
import zio.metrics.prometheus.exporters.Exporters
import zio.magic._
import scalapb.zio_grpc.{Server => GrpcServer}
import org.http4s.server.{Server => HttpServer}
import eu.timepit.refined.auto._

object Main extends App {

  type CommonEnvironment = Clock with Console with Blocking with Logging with Registry with Exporters

  val commonLayer = ZLayer.fromMagic[CommonEnvironment](
    Clock.live,
    Console.live,
    Blocking.live,
    Slf4jLogger.make((_, message) => message),
    Registry.live,
    Exporters.live
  )

  type AppEnvironment = CommonEnvironment
    with JwtAuthenticator with DbConnection with SubscriptionRepo with SubscriptionEventRepo with SubscriptionDomain
    with SubscriptionEventProducer with LoggingSystem with LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler
    with GrpcServer with Has[HttpServer] with CdcHandler

  private def createAppLayer(appConfig: AppAllConfig): ZLayer[Any, Throwable, AppEnvironment] = {
    ZLayer.fromMagic[AppEnvironment](
      commonLayer,
      JwtAuthenticator.live(appConfig.jwt),
      DbConnection.create(appConfig.db.connection),
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
      PostgresCdc.create(appConfig.db, SubscriptionEventProducer.processAndSend)
    )
  }

  type SvcAppEnvironment = CommonEnvironment
    with JwtAuthenticator with DbConnection with SubscriptionRepo with SubscriptionEventRepo with SubscriptionDomain
    with LoggingSystem with LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler with GrpcServer
    with Has[HttpServer]

  private def createSvcAppLayer(appConfig: AppSvcConfig): ZLayer[Any, Throwable, SvcAppEnvironment] = {
    ZLayer.fromMagic[SvcAppEnvironment](
      commonLayer,
      JwtAuthenticator.live(appConfig.jwt),
      DbConnection.create(appConfig.db.connection),
      SubscriptionRepo.live,
      SubscriptionEventRepo.live,
      SubscriptionDomain.live,
      LogbackLoggingSystem.create(),
      LoggingSystemGrpcApi.live,
      SubscriptionGrpcApiHandler.live,
      HttpApiServer.create(appConfig.restApi),
      GrpcApiServer.create(appConfig.grpcApi)
    )
  }

  type CdcAppEnvironment = CommonEnvironment
    with DbConnection with SubscriptionEventProducer with Has[HttpServer] with CdcHandler

  private def createCdcAppLayer(appConfig: AppCdcConfig): ZLayer[Any, Throwable, CdcAppEnvironment] = {
    ZLayer.fromMagic[CdcAppEnvironment](
      commonLayer,
      DbConnection.create(appConfig.db.connection),
      KafkaProducer.create(appConfig.kafka),
      SubscriptionEventProducer.create(appConfig.kafka.subscriptionTopic),
      HttpApiServer.create(appConfig.restApi),
      PostgresCdc.create(appConfig.db, SubscriptionEventProducer.processAndSend)
    )
  }

  private def getConfigAndLayer(): ZIO[Any, Throwable, (AppConfig, ZLayer[Any, Throwable, CommonEnvironment])] = {
    for {
      config <- ZIO.succeed(ConfigFactory.load())

      mode <- AppMode.getMode(config)

      res <- mode match {
        case AppMode.All => AppConfig.getConfig[AppAllConfig](config).map(c => c -> createAppLayer(c))
        case AppMode.Svc => AppConfig.getConfig[AppSvcConfig](config).map(c => c -> createSvcAppLayer(c))
        case AppMode.Cdc => AppConfig.getConfig[AppCdcConfig](config).map(c => c -> createCdcAppLayer(c))
      }
    } yield res
  }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val result: ZIO[zio.ZEnv, Throwable, Nothing] = for {

      (appConfig, appLayer) <- getConfigAndLayer()

      runtime: ZIO[CommonEnvironment, Throwable, Nothing] = ZIO.runtime[CommonEnvironment].flatMap {
        implicit rts: Runtime[CommonEnvironment] =>
          Logging.debug(s"app mode: ${appConfig.getClass.getSimpleName.substring(0, 6)}") *>
            PrometheusMetricsExporter.create(appConfig.prometheus) *>
            ZIO.never
      }

      program <- runtime.provideCustomLayer[Throwable, CommonEnvironment](appLayer)
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
