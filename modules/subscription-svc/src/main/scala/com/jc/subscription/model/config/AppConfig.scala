package com.jc.subscription.model.config

import com.jc.auth.JwtConfig
import com.typesafe.config.Config
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.error.ConfigReaderException
import pureconfig.generic.semiauto._
import zio.ZIO

sealed trait AppConfig {
  def restApi: HttpApiConfig
  def prometheus: PrometheusConfig
}

object AppConfig {

  def getConfig[C <: AppConfig](config: Config)(implicit
    r: ConfigReader[C]): ZIO[Any, ConfigReaderException[Nothing], C] =
    ZIO.fromEither(ConfigSource.fromConfig(config).load[C]).mapError(ConfigReaderException.apply)
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
}

final case class AppCdcConfig(kafka: KafkaConfig, restApi: HttpApiConfig, prometheus: PrometheusConfig, db: DbCdcConfig)
    extends AppConfig

object AppCdcConfig {
  implicit lazy val appConfigReader = deriveReader[AppCdcConfig]
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
}
