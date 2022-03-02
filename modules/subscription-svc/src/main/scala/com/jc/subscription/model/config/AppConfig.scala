package com.jc.subscription.model.config

import com.jc.auth.JwtConfig
import com.typesafe.config.Config
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.error.ConfigReaderException
import pureconfig.generic.semiauto._
import zio.{IO, ZIO}
import zio.config._
import ConfigDescriptor._

sealed trait AppConfig {
  def restApi: HttpApiConfig
  def prometheus: PrometheusConfig
}

object AppConfig {

  def getConfig[C <: AppConfig](config: Config)(implicit
    r: ConfigReader[C]): ZIO[Any, ConfigReaderException[Nothing], C] =
    ZIO.fromEither(ConfigSource.fromConfig(config).load[C]).mapError(ConfigReaderException.apply)

  def readConfig[C <: AppConfig](config: zio.config.ConfigSource)(implicit
    r: ConfigDescriptor[C]): IO[ReadError[String], C] = {
    read(r from config)
  }
}

final case class AppAllConfig(
  kafka: KafkaConfig,
  grpcApi: HttpApiConfig,
  restApi: HttpApiConfig,
  prometheus: PrometheusConfig,
  jwt: JwtConfig,
  db: DbCdcConfig)
    extends AppConfig

object AppAllConfig {
  implicit lazy val appConfigReader = deriveReader[AppAllConfig]

  implicit val appAllConfigDescription = (nested("kafka")(KafkaConfig.kafkaConfigDescription) zip
    nested("grpc-api")(HttpApiConfig.httpConfigDescription) zip
    nested("rest-api")(HttpApiConfig.httpConfigDescription) zip
    nested("prometheus")(PrometheusConfig.prometheusConfigDescription) zip
    nested("jwt")(JwtConfig.jwtConfigDescription) zip
    nested("db")(DbCdcConfig.dbCdcConfigDescription)).to[AppAllConfig]
}

final case class AppCdcConfig(kafka: KafkaConfig, restApi: HttpApiConfig, prometheus: PrometheusConfig, db: DbCdcConfig)
    extends AppConfig

object AppCdcConfig {
  implicit lazy val appConfigReader = deriveReader[AppCdcConfig]

  implicit val appCdcConfigDescriptor = (nested("kafka")(KafkaConfig.kafkaConfigDescription) zip
    nested("rest-api")(HttpApiConfig.httpConfigDescription) zip
    nested("prometheus")(PrometheusConfig.prometheusConfigDescription) zip
    nested("db")(DbCdcConfig.dbCdcConfigDescription)).to[AppCdcConfig]
}

final case class AppSvcConfig(
  grpcApi: HttpApiConfig,
  restApi: HttpApiConfig,
  prometheus: PrometheusConfig,
  jwt: JwtConfig,
  db: DbConfig)
    extends AppConfig

object AppSvcConfig {
  implicit lazy val appConfigReader = deriveReader[AppSvcConfig]

  implicit val appSvcConfigDescriptor = (nested("grpc-api")(HttpApiConfig.httpConfigDescription) zip
    nested("rest-api")(HttpApiConfig.httpConfigDescription) zip
    nested("prometheus")(PrometheusConfig.prometheusConfigDescription) zip
    nested("jwt")(JwtConfig.jwtConfigDescription) zip
    nested("db")(DbConfig.dbConfigDescription)).to[AppSvcConfig]
}
