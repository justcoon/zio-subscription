package com.jc.auth

import eu.timepit.refined.types.numeric.PosLong
import eu.timepit.refined.types.string.NonEmptyString
import zio.config.toKebabCase
import zio.config.refined._
import zio.config.magnolia.descriptor

final case class JwtConfig(secret: NonEmptyString, expiration: PosLong, issuer: Option[NonEmptyString] = None)

object JwtConfig {
  implicit val jwtConfigDescription = descriptor[JwtConfig].mapKey(toKebabCase)
}
