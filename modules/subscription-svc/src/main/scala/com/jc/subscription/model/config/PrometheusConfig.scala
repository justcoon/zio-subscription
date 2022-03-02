package com.jc.subscription.model.config

import eu.timepit.refined.types.net.NonSystemPortNumber
import pureconfig.generic.semiauto.deriveReader
import zio.config.toKebabCase

final case class PrometheusConfig(port: NonSystemPortNumber)

object PrometheusConfig {
  import eu.timepit.refined.pureconfig._
  implicit lazy val configReader = deriveReader[PrometheusConfig]

  import zio.config.refined._
  import zio.config.magnolia.descriptor

  implicit val prometheusConfigDescription = descriptor[PrometheusConfig].mapKey(toKebabCase)
}
