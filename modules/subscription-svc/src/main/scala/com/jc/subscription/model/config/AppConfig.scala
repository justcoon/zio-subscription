package com.jc.subscription.model.config

import com.jc.auth.JwtConfig
import com.typesafe.config.Config
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.error.ConfigReaderException
import pureconfig.generic.semiauto._
import zio.ZIO
import zio.config.typesafe._
import zio.config.syntax._
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor._

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

  implicit val appAllConfigDescription = descriptor[AppAllConfig].mapKey(toKebabCase)
}

final case class AppCdcConfig(kafka: KafkaConfig, restApi: HttpApiConfig, prometheus: PrometheusConfig, db: DbCdcConfig)
    extends AppConfig

object AppCdcConfig {
  implicit lazy val appConfigReader = deriveReader[AppCdcConfig]

  implicit val appCdcConfigDescriptor = descriptor[AppCdcConfig].mapKey(toKebabCase)
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
  import HttpApiConfig._
  import PrometheusConfig._
  import JwtConfig._
  import DbConfig._
  implicit val appSvcConfigDescriptor = descriptor[AppSvcConfig].mapKey(toKebabCase)
}

//final case class AppSvcConfig2(
////  grpcApi: HttpApiConfig
////  restApi: HttpApiConfig,
////  prometheus: PrometheusConfig,
//  jwt: JwtConfig,
//  jw2: String,
//  db: DbConfig
//)
//
//object AppSvcConfig2 {
//  import DbConfig._
////  import JwtConfig._
////  import PrometheusConfig._
//  implicit val configDescriptor = descriptor[AppSvcConfig2]
//}
