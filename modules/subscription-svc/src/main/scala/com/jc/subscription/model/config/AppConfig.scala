package com.jc.subscription.model.config

import com.jc.auth.JwtConfig

sealed trait AppConfig {
  def restApi: HttpApiConfig
  def prometheus: PrometheusConfig
  def db: DbConfig
  def mode: AppMode
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

  implicit val appAllConfig = (KafkaConfig.kafkaConfig.nested("kafka") zip
    HttpApiConfig.httpConfig.nested("grpc-api") zip
    HttpApiConfig.httpConfig.nested("rest-api") zip
    PrometheusConfig.prometheusConfig.nested("prometheus") zip
    JwtConfig.jwtConfig.nested("jwt") zip
    DbCdcConfig.dbCdcConfig.nested("db")).map { case (kafka, grpcApi, restApi, prometheus, jwt, db) =>
    AppAllConfig(kafka, grpcApi, restApi, prometheus, jwt, db)
  }
}

final case class AppCdcConfig(kafka: KafkaConfig, restApi: HttpApiConfig, prometheus: PrometheusConfig, db: DbCdcConfig)
    extends AppConfig {
  override val mode = AppMode.cdc
}

object AppCdcConfig {

  implicit val appCdcConfig = (KafkaConfig.kafkaConfig.nested("kafka") zip
    HttpApiConfig.httpConfig.nested("rest-api") zip
    PrometheusConfig.prometheusConfig.nested("prometheus") zip
    DbCdcConfig.dbCdcConfig.nested("db")).map { case (kafka, restApi, prometheus, db) =>
    AppCdcConfig(kafka, restApi, prometheus, db)
  }
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

  implicit val appSvcConfig = (HttpApiConfig.httpConfig.nested("grpc-api") zip
    HttpApiConfig.httpConfig.nested("rest-api") zip
    PrometheusConfig.prometheusConfig.nested("prometheus") zip
    JwtConfig.jwtConfig.nested("jwt") zip
    DbConConfig.dbConfig.nested("db")).map { case (grpcApi, restApi, prometheus, jwt, db) =>
    AppSvcConfig(grpcApi, restApi, prometheus, jwt, db)
  }
}
