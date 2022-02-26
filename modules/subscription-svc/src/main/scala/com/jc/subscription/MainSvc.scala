package com.jc.subscription

import com.jc.auth.JwtAuthenticator
import com.jc.logging.api.{LoggingSystemGrpcApi, LoggingSystemGrpcApiHandler}
import com.jc.logging.{LogbackLoggingSystem, LoggingSystem}
import com.jc.subscription.model.config.AppSvcConfig
import com.jc.subscription.module.api.{GrpcApiServer, HttpApiServer, SubscriptionGrpcApiHandler}
import com.jc.subscription.module.db.DbConnection
import com.jc.subscription.module.domain.SubscriptionDomain
import com.jc.subscription.module.metrics.PrometheusMetricsExporter
import com.jc.subscription.module.repo.{SubscriptionEventRepo, SubscriptionRepo}
import org.http4s.server.{Server => HttpServer}
import scalapb.zio_grpc.{Server => GrpcServer}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger
import zio.magic._
import zio.metrics.prometheus._
import zio.metrics.prometheus.exporters.Exporters
import zio.metrics.prometheus.helpers._

object MainSvc extends App {

  type AppEnvironment = Clock
    with Console with Blocking with JwtAuthenticator with DbConnection with SubscriptionRepo with SubscriptionEventRepo
    with SubscriptionDomain with LoggingSystem with LoggingSystemGrpcApiHandler with SubscriptionGrpcApiHandler
    with GrpcServer with Has[HttpServer] with Logging with Registry with Exporters

  private def createAppLayer(appConfig: AppSvcConfig): ZLayer[Any, Throwable, AppEnvironment] = {
    ZLayer.fromMagic[AppEnvironment](
      Clock.live,
      Console.live,
      Blocking.live,
      Slf4jLogger.make((_, message) => message),
      JwtAuthenticator.live(appConfig.jwt),
      DbConnection.create(appConfig.db.connection),
      SubscriptionRepo.live,
      SubscriptionEventRepo.live,
      SubscriptionDomain.live,
      LogbackLoggingSystem.create(),
      LoggingSystemGrpcApi.live,
      SubscriptionGrpcApiHandler.live,
      HttpApiServer.create(appConfig.restApi),
      GrpcApiServer.create(appConfig.grpcApi),
      Registry.live,
      Exporters.live
    )
  }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val result: ZIO[zio.ZEnv, Throwable, Nothing] = for {
      appConfig <- AppSvcConfig.getConfig

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
