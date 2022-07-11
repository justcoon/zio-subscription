package com.jc.subscription

import com.jc.subscription.model.config.{AppAllConfig, AppCdcConfig, AppConfig, AppMode, AppSvcConfig}
import com.jc.subscription.module.api.{GrpcApiServer, HttpApiServer, SubscriptionGrpcApiHandler}
import com.jc.auth.{JwtAuthenticator, PdiJwtAuthenticator}
import com.jc.cdc.CdcHandler
import com.jc.logging.{LogbackLoggingSystem, LoggingSystem}
import com.jc.logging.api.LoggingSystemGrpcApiHandler
import com.jc.subscription.module.db.cdc.PostgresCdc
import com.jc.subscription.module.db.{DbConnection, DbInit}
import com.jc.subscription.module.domain.{LiveSubscriptionDomainService, SubscriptionDomainService}
import com.jc.subscription.module.event.{LiveSubscriptionEventProducer, SubscriptionEventProducer}
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
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  type CommonEnvironment = DbConnection

  type AppEnvironment = CommonEnvironment
    with JwtAuthenticator with SubscriptionRepo[DbConnection] with SubscriptionEventRepo[DbConnection]
    with SubscriptionDomainService with SubscriptionEventProducer with LoggingSystem with LoggingSystemGrpcApiHandler
    with SubscriptionGrpcApiHandler with GrpcServer with HttpServer with CdcHandler

  private val logger = Runtime.removeDefaultLoggers >>> SLF4J.slf4j(
    zio.LogLevel.Debug,
    LogFormat.line |-| LogFormat.cause,
    _ match {
      case Trace(location, _, _) => location
      case _ => "zio-subscription-logger"
    }
  )

  private def createAppConfigAndLayer(config: ConfigSource): Task[(AppAllConfig, TaskLayer[AppEnvironment])] = {
    AppConfig.readConfig[AppAllConfig](config).map { appConfig =>
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
        LiveSubscriptionEventProducer.make(appConfig.kafka.subscriptionTopic),
        HttpApiServer.make(appConfig.restApi),
        GrpcApiServer.make(appConfig.grpcApi),
        PostgresCdc.make(appConfig.db, SubscriptionEventProducer.processAndSend)
      )
    }
  }

  type SvcAppEnvironment = CommonEnvironment
    with JwtAuthenticator with SubscriptionRepo[DbConnection] with SubscriptionEventRepo[DbConnection]
    with SubscriptionDomainService with LoggingSystem with LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler
    with GrpcServer with HttpServer

  private def createSvcAppConfigAndLayer(config: ConfigSource): Task[(AppSvcConfig, TaskLayer[SvcAppEnvironment])] = {
    AppConfig.readConfig[AppSvcConfig](config).map { appConfig =>
      appConfig -> ZLayer.make[SvcAppEnvironment](
        PdiJwtAuthenticator.make(appConfig.jwt),
        DbConnection.make(appConfig.db.connection),
        LiveSubscriptionRepo.layer,
        LiveSubscriptionEventRepo.layer,
        LiveSubscriptionDomainService.layer,
        LogbackLoggingSystem.make(),
        LoggingSystemGrpcApiHandler.layer,
        SubscriptionGrpcApiHandler.layer,
        HttpApiServer.make(appConfig.restApi),
        GrpcApiServer.make(appConfig.grpcApi)
      )
    }
  }

  type CdcAppEnvironment = CommonEnvironment with SubscriptionEventProducer with HttpServer with CdcHandler

  private def createCdcAppConfigAndLayer(config: ConfigSource): Task[(AppCdcConfig, TaskLayer[CdcAppEnvironment])] = {
    AppConfig.readConfig[AppCdcConfig](config).map { appConfig =>
      appConfig -> ZLayer.make[CdcAppEnvironment](
        DbConnection.make(appConfig.db.connection),
        KafkaProducer.make(appConfig.kafka),
        LiveSubscriptionEventProducer.make(appConfig.kafka.subscriptionTopic),
        HttpApiServer.make(appConfig.restApi),
        PostgresCdc.make(appConfig.db, SubscriptionEventProducer.processAndSend)
      )
    }
  }

  private val appConfigAndLayer: ZIO[Any, Throwable, (AppConfig, ZLayer[Any, Throwable, CommonEnvironment])] = {
    for {
      config <- ZIO.succeed(ConfigSource.fromResourcePath.memoize)

      mode <- AppMode.readMode(config)

      res <- mode match {
        case AppMode.`all` => createAppConfigAndLayer(config)
        case AppMode.`svc` => createSvcAppConfigAndLayer(config)
        case AppMode.`cdc` => createCdcAppConfigAndLayer(config)
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
  }.provide(logger)
}
