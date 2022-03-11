package com.jc.subscription.model.config

import com.jc.auth.JwtConfig
import zio.IO
import zio.config._
import ConfigDescriptor._

sealed trait AppConfig {
  def restApi: HttpApiConfig
  def prometheus: PrometheusConfig
  def db: DbConfig
  def mode: AppMode
}

object AppConfig {

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
    extends AppConfig {
  override val mode = AppMode.all
}

object AppAllConfig {

  implicit val appAllConfigDescription = (nested("kafka")(KafkaConfig.kafkaConfigDescription) zip
    nested("grpc-api")(HttpApiConfig.httpConfigDescription) zip
    nested("rest-api")(HttpApiConfig.httpConfigDescription) zip
    nested("prometheus")(PrometheusConfig.prometheusConfigDescription) zip
    nested("jwt")(JwtConfig.jwtConfigDescription) zip
    nested("db")(DbCdcConfig.dbCdcConfigDescription)).to[AppAllConfig]
}

final case class AppCdcConfig(kafka: KafkaConfig, restApi: HttpApiConfig, prometheus: PrometheusConfig, db: DbCdcConfig)
    extends AppConfig {
  override val mode = AppMode.cdc
}

object AppCdcConfig {

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
  db: DbConConfig)
    extends AppConfig {
  override val mode = AppMode.svc
}

object AppSvcConfig {

  implicit val appSvcConfigDescriptor = (nested("grpc-api")(HttpApiConfig.httpConfigDescription) zip
    nested("rest-api")(HttpApiConfig.httpConfigDescription) zip
    nested("prometheus")(PrometheusConfig.prometheusConfigDescription) zip
    nested("jwt")(JwtConfig.jwtConfigDescription) zip
    nested("db")(DbConConfig.dbConfigDescription)).to[AppSvcConfig]
}
