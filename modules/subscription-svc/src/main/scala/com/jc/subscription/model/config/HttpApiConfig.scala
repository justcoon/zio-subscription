package com.jc.subscription.model.config

import eu.timepit.refined.types.net.NonSystemPortNumber
import zio.config.refined._
import zio.config.magnolia.deriveConfig

final case class HttpApiConfig(address: IpAddress, port: NonSystemPortNumber)

object HttpApiConfig {
  implicit val httpConfig = deriveConfig[HttpApiConfig]
}
