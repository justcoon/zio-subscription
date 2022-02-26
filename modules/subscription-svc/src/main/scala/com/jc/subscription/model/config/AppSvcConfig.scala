package com.jc.subscription.model.config

import com.jc.auth.JwtConfig
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.semiauto.deriveReader
import zio.ZIO

final case class AppSvcConfig(
  grpcApi: HttpApiConfig,
  restApi: HttpApiConfig,
  prometheus: PrometheusConfig,
  jwt: JwtConfig,
  db: DbConfig)

object AppSvcConfig {
  implicit lazy val appConfigReader = deriveReader[AppSvcConfig]

  val getConfig: ZIO[Any, ConfigReaderException[Nothing], AppSvcConfig] =
    ZIO.fromEither(ConfigSource.default.load[AppSvcConfig]).mapError(ConfigReaderException.apply)
}
