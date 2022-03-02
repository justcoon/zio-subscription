package com.jc.subscription.model.config

import eu.timepit.refined.types.net.NonSystemPortNumber
import pureconfig.generic.semiauto.deriveReader
import zio.config.toKebabCase
import zio.config.refined._
import zio.config.magnolia.descriptor

final case class PrometheusConfig(port: NonSystemPortNumber)

object PrometheusConfig {
  import eu.timepit.refined.pureconfig._
  implicit lazy val configReader = deriveReader[PrometheusConfig]

  implicit val prometheusConfigDescription = descriptor[PrometheusConfig].mapKey(toKebabCase)
}
