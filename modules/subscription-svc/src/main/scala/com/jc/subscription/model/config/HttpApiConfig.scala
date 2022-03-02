package com.jc.subscription.model.config

import eu.timepit.refined.types.net.NonSystemPortNumber
import pureconfig.generic.semiauto.deriveReader
import zio.config.toKebabCase

final case class HttpApiConfig(address: IpAddress, port: NonSystemPortNumber)

object HttpApiConfig {
  import eu.timepit.refined.pureconfig._
  implicit lazy val configReader = deriveReader[HttpApiConfig]

  import zio.config.refined._
  import zio.config.magnolia.descriptor

  implicit val configDescription = descriptor[HttpApiConfig].mapKey(toKebabCase)
}
