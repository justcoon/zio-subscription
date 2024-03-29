package com.jc.subscription

import com.jc.subscription.model.config.{AppAllConfig, AppCdcConfig, AppConfig, AppMode, AppSvcConfig}
import com.jc.subscription.module.api.{GrpcApiServer, HttpApiServer, SubscriptionGrpcApiHandler}
import com.jc.auth.{JwtAuthenticator, PdiJwtAuthenticator}
import com.jc.cdc.CdcHandler
import com.jc.logging.{LogbackLoggingSystem, LoggingSystem}
import com.jc.logging.api.LoggingSystemGrpcApiHandler
import com.jc.subscription.module.Logger
import com.jc.subscription.module.Metrics
import com.jc.subscription.module.Metrics.JvmMetrics
import com.jc.subscription.module.db.cdc.PostgresCdc
import com.jc.subscription.module.db.{DbConnection, DbInit}
import com.jc.subscription.module.domain.{LiveSubscriptionDomainService, SubscriptionDomainService}
import com.jc.subscription.module.event.{KafkaSubscriptionEventProducer, SubscriptionEventProducer}
import com.jc.subscription.module.kafka.KafkaProducer
import com.jc.subscription.module.repo.{
  LiveSubscriptionEventRepo,
  LiveSubscriptionRepo,
  SubscriptionEventRepo,
  SubscriptionRepo
}
import zio._
import zio.config._
import zio.config.typesafe._
import scalapb.zio_grpc.{Server => GrpcServer}
import org.http4s.server.{Server => HttpServer}
import eu.timepit.refined.auto._
import zio.ZIOAppDefault
import zio.metrics.connectors.prometheus.PrometheusPublisher

object Main extends ZIOAppDefault {

  type CommonEnvironment = DbConnection with PrometheusPublisher with JvmMetrics

  type AppEnvironment = CommonEnvironment
    with JwtAuthenticator with SubscriptionRepo[DbConnection] with SubscriptionEventRepo[DbConnection]
    with SubscriptionDomainService with SubscriptionEventProducer with LoggingSystem with LoggingSystemGrpcApiHandler
    with SubscriptionGrpcApiHandler with GrpcServer with HttpServer with CdcHandler

  private def createAppConfigAndLayer: Task[(AppAllConfig, TaskLayer[AppEnvironment])] = {
    ZIO.config(AppAllConfig.appAllConfig).map { appConfig =>
      appConfig -> ZLayer.make[AppEnvironment](
        PdiJwtAuthenticator.make(appConfig.jwt),
        DbConnection.make(appConfig.db.connection),
        LiveSubscriptionRepo.layer,
        LiveSubscriptionEventRepo.layer,
        LiveSubscriptionDomainService.layer,
        LogbackLoggingSystem.make(),
        LoggingSystemGrpcApiHandler.layer,
        SubscriptionGrpcApiHandler.layer,
        KafkaProducer.make(appConfig.kafka),
        KafkaSubscriptionEventProducer.make(appConfig.kafka.subscriptionTopic),
        PostgresCdc.make(appConfig.db, SubscriptionEventProducer.processAndSend),
        ZLayer.succeed(appConfig.grpcApi) >>> GrpcApiServer.layer,
        ZLayer.succeed(appConfig.restApi) >>> HttpApiServer.layer,
        Metrics.layer
      )
    }
  }

  type SvcAppEnvironment = CommonEnvironment
    with JwtAuthenticator with SubscriptionRepo[DbConnection] with SubscriptionEventRepo[DbConnection]
    with SubscriptionDomainService with LoggingSystem with LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler
    with GrpcServer with HttpServer

  private def createSvcAppConfigAndLayer: Task[(AppSvcConfig, TaskLayer[SvcAppEnvironment])] = {
    ZIO.config(AppSvcConfig.appSvcConfig).map { appConfig =>
      appConfig -> ZLayer.make[SvcAppEnvironment](
        PdiJwtAuthenticator.make(appConfig.jwt),
        DbConnection.make(appConfig.db.connection),
        LiveSubscriptionRepo.layer,
        LiveSubscriptionEventRepo.layer,
        LiveSubscriptionDomainService.layer,
        LogbackLoggingSystem.make(),
        LoggingSystemGrpcApiHandler.layer,
        SubscriptionGrpcApiHandler.layer,
        ZLayer.succeed(appConfig.grpcApi) >>> GrpcApiServer.layer,
        ZLayer.succeed(appConfig.restApi) >>> HttpApiServer.layer,
        Metrics.layer
      )
    }
  }

  type CdcAppEnvironment = CommonEnvironment with SubscriptionEventProducer with HttpServer with CdcHandler

  private def createCdcAppConfigAndLayer: Task[(AppCdcConfig, TaskLayer[CdcAppEnvironment])] = {
    ZIO.config(AppCdcConfig.appCdcConfig).map { appConfig =>
      appConfig -> ZLayer.make[CdcAppEnvironment](
        DbConnection.make(appConfig.db.connection),
        KafkaProducer.make(appConfig.kafka),
        KafkaSubscriptionEventProducer.make(appConfig.kafka.subscriptionTopic),
        PostgresCdc.make(appConfig.db, SubscriptionEventProducer.processAndSend),
        ZLayer.succeed(appConfig.restApi) >>> HttpApiServer.layer,
        Metrics.layer
      )
    }
  }

  private val appConfigAndLayer: ZIO[Any, Throwable, (AppConfig, ZLayer[Any, Throwable, CommonEnvironment])] = {
    for {
      mode <- AppMode.readMode

      res <- mode match {
        case AppMode.`all` => createAppConfigAndLayer
        case AppMode.`svc` => createSvcAppConfigAndLayer
        case AppMode.`cdc` => createCdcAppConfigAndLayer
      }
    } yield res

  }

  override def run: ZIO[Scope, Any, ExitCode] = appConfigAndLayer.flatMap { case (appConfig, layer) =>
    val run: ZIO[CommonEnvironment, Throwable, ExitCode] =
      for {
        _ <- ZIO.logDebug(s"app mode: ${appConfig.mode}")
        _ <- DbInit.run(appConfig.db.connection)
        _ <- ZIO.never
      } yield ExitCode.success

    run.provide(layer)
  }.provide(Logger.layer ++ Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath()))
}
