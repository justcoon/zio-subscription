package com.jc.auth.api

import com.jc.auth.JwtAuthenticator
import org.http4s.{Headers, Status}
import org.typelevel.ci.CIString
import zio.ZIO

object HttpJwtAuth {

  private val AuthHeader = CIString(JwtAuthenticator.AuthHeader)

  def authenticated(authenticator: JwtAuthenticator): Headers => ZIO[Any, Status, String] = headers =>
    for {
      rawToken <- ZIO.getOrFailWith(Status.Unauthorized)(headers.get(AuthHeader).map(_.head))
      maybeSubject <- authenticator.authenticated(JwtAuthenticator.sanitizeBearerAuthToken(rawToken.value))
      subject <- ZIO.getOrFailWith(Status.Unauthorized)(maybeSubject)
    } yield subject

}
