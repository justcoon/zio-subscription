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
import io.grpc.StatusException
import scalapb.zio_grpc.RequestContext
import zio.{UIO, ZIO, ZLayer}

final class LoggingSystemGrpcApiHandler(loggingSystem: LoggingSystem, authenticator: JwtAuthenticator)
    extends RCLoggingSystemApiService {

  private val authenticated = GrpcJwtAuth.authenticated(authenticator)

  def getSupportedLogLevels: UIO[Seq[LogLevel]] =
    loggingSystem.getSupportedLogLevels.map { levels =>
      levels.map(LoggingSystemGrpcApiHandler.logLevelMapping.toLogger).toSeq
    }

  def toApiLoggerConfiguration(configuration: LoggingSystem.LoggerConfiguration): LoggerConfiguration =
    LoggerConfiguration(
      configuration.name,
      LoggingSystemGrpcApiHandler.logLevelMapping.toLogger(configuration.effectiveLevel),
      configuration.configuredLevel.flatMap(LoggingSystemGrpcApiHandler.logLevelMapping.toLogger.get)
    )

  override def setLoggerConfiguration(
    request: SetLoggerConfigurationReq,
    context: RequestContext): ZIO[Any, StatusException, LoggerConfigurationRes] = {
    for {
      _ <- authenticated(context)
      res <- loggingSystem.setLogLevel(
        request.name,
        request.level.flatMap(LoggingSystemGrpcApiHandler.logLevelMapping.fromLogger.get))
      levels <- getSupportedLogLevels
      configuration <-
        if (res) {
          loggingSystem.getLoggerConfiguration(request.name)
        } else ZIO.succeed(None)
    } yield LoggerConfigurationRes(configuration.map(toApiLoggerConfiguration), levels)
  }

  override def getLoggerConfiguration(
    request: GetLoggerConfigurationReq,
    context: RequestContext): ZIO[Any, StatusException, LoggerConfigurationRes] = {
    for {
      _ <- authenticated(context)
      levels <- getSupportedLogLevels
      configuration <- loggingSystem.getLoggerConfiguration(request.name)
    } yield LoggerConfigurationRes(configuration.map(toApiLoggerConfiguration), levels)
  }

  override def getLoggerConfigurations(
    request: GetLoggerConfigurationsReq,
    context: RequestContext): ZIO[Any, StatusException, LoggerConfigurationsRes] = {
    for {
      _ <- authenticated(context)
      levels <- getSupportedLogLevels
      configurations <- loggingSystem.getLoggerConfigurations
    } yield LoggerConfigurationsRes(configurations.map(toApiLoggerConfiguration), levels)
  }
}

object LoggingSystemGrpcApiHandler {

  /** using [[LoggingSystem.LogLevelMapping]] for api <-> logging system
    *  level mapping
    *
    * it is specially handled like: log level not defined
    */
  final val logLevelMapping: LoggingSystem.LogLevelMapping[LogLevel] = LoggingSystem.LogLevelMapping(
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

  val layer: ZLayer[LoggingSystem with JwtAuthenticator, Nothing, LoggingSystemGrpcApiHandler] = {
    ZLayer.fromZIO {
      for {
        loggingSystem <- ZIO.service[LoggingSystem]
        authenticator <- ZIO.service[JwtAuthenticator]
      } yield new LoggingSystemGrpcApiHandler(loggingSystem, authenticator)
    }
  }
}
