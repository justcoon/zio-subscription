package com.jc.subscription.model.config

import eu.timepit.refined.types.net.NonSystemPortNumber
import zio.config.refined._
import zio.config.magnolia.deriveConfig

final case class PrometheusConfig(port: NonSystemPortNumber)

object PrometheusConfig {
  implicit val prometheusConfig = deriveConfig[PrometheusConfig]
}
