package com.jc.auth

import eu.timepit.refined.types.numeric.PosLong
import eu.timepit.refined.types.string.NonEmptyString
import zio.config.refined._
import zio.config.magnolia.deriveConfig

final case class JwtConfig(secret: NonEmptyString, expiration: PosLong, issuer: Option[NonEmptyString] = None)

object JwtConfig {
  implicit val jwtConfig = deriveConfig[JwtConfig]
}
