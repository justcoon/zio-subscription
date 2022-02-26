package com.jc.subscription.model.config

import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.semiauto.deriveReader
import zio.ZIO

final case class AppCdcConfig(kafka: KafkaConfig, restApi: HttpApiConfig, prometheus: PrometheusConfig, db: DbCdcConfig)

object AppCdcConfig {
  implicit lazy val appConfigReader = deriveReader[AppCdcConfig]

  val getConfig: ZIO[Any, ConfigReaderException[Nothing], AppCdcConfig] =
    ZIO.fromEither(ConfigSource.default.load[AppCdcConfig]).mapError(ConfigReaderException.apply)
}
