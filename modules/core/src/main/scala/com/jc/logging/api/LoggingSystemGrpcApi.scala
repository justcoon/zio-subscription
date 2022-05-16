package com.jc.logging.api

import com.jc.auth.JwtAuthenticator
import com.jc.auth.api.GrpcJwtAuth
import com.jc.logging.LoggingSystem
import com.jc.logging.proto.{
  GetLoggerConfigurationReq,
  GetLoggerConfigurationsReq,
  LogLevel,
  LoggerConfiguration,
  LoggerConfigurationRes,
  LoggerConfigurationsRes,
  SetLoggerConfigurationReq
}
import com.jc.logging.proto.ZioLoggingSystemApi.RCLoggingSystemApiService
import io.grpc.Status
import scalapb.zio_grpc.RequestContext
import zio.{UIO, ZIO, ZLayer}

object LoggingSystemGrpcApi {

  /** using [[LoggingSystem.LogLevelMapping]] for api <-> logging system
    *  level mapping
    *
    * it is specially handled like: log level not defined
    */
  private val logLevelMapping: LoggingSystem.LogLevelMapping[LogLevel] = LoggingSystem.LogLevelMapping(
    Seq(
      (LoggingSystem.LogLevel.TRACE, LogLevel.TRACE),
      (LoggingSystem.LogLevel.DEBUG, LogLevel.DEBUG),
      (LoggingSystem.LogLevel.INFO, LogLevel.INFO),
      (LoggingSystem.LogLevel.WARN, LogLevel.WARN),
      (LoggingSystem.LogLevel.ERROR, LogLevel.ERROR),
      (LoggingSystem.LogLevel.FATAL, LogLevel.FATAL),
      (LoggingSystem.LogLevel.OFF, LogLevel.OFF)
    )
  )

  final case class LiveLoggingSystemGrpcService(loggingSystem: LoggingSystem, authenticator: JwtAuthenticator)
      extends RCLoggingSystemApiService[Any] {

    def getSupportedLogLevels: UIO[Seq[LogLevel]] =
      loggingSystem.getSupportedLogLevels.map { levels =>
        levels.map(logLevelMapping.toLogger).toSeq
      }

    def toApiLoggerConfiguration(configuration: LoggingSystem.LoggerConfiguration): LoggerConfiguration =
      LoggerConfiguration(
        configuration.name,
        logLevelMapping.toLogger(configuration.effectiveLevel),
        configuration.configuredLevel.flatMap(logLevelMapping.toLogger.get)
      )

    override def setLoggerConfiguration(
      request: SetLoggerConfigurationReq): ZIO[Any with RequestContext, Status, LoggerConfigurationRes] = {
      for {
        _ <- GrpcJwtAuth.authenticated(authenticator)
        res <- loggingSystem.setLogLevel(request.name, request.level.flatMap(logLevelMapping.fromLogger.get))
        levels <- getSupportedLogLevels
        configuration <-
          if (res) {
            loggingSystem.getLoggerConfiguration(request.name)
          } else ZIO.succeed(None)
      } yield LoggerConfigurationRes(configuration.map(toApiLoggerConfiguration), levels)
    }

    override def getLoggerConfiguration(
      request: GetLoggerConfigurationReq): ZIO[Any with RequestContext, Status, LoggerConfigurationRes] = {
      for {
        _ <- GrpcJwtAuth.authenticated(authenticator)
        levels <- getSupportedLogLevels
        configuration <- loggingSystem.getLoggerConfiguration(request.name)
      } yield LoggerConfigurationRes(configuration.map(toApiLoggerConfiguration), levels)
    }

    override def getLoggerConfigurations(
      request: GetLoggerConfigurationsReq): ZIO[Any with RequestContext, Status, LoggerConfigurationsRes] = {
      for {
        _ <- GrpcJwtAuth.authenticated(authenticator)
        levels <- getSupportedLogLevels
        configurations <- loggingSystem.getLoggerConfigurations
      } yield LoggerConfigurationsRes(configurations.map(toApiLoggerConfiguration), levels)
    }
  }

  val live: ZLayer[LoggingSystem with JwtAuthenticator, Nothing, LoggingSystemGrpcApiHandler] = {
    ZLayer.fromZIO {
      for {
        loggingSystem <- ZIO.service[LoggingSystem]
        authenticator <- ZIO.service[JwtAuthenticator]
      } yield LiveLoggingSystemGrpcService(loggingSystem, authenticator)
    }
  }
}
