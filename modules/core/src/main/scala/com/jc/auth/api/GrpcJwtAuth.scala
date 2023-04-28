package com.jc.auth.api

import com.jc.auth.JwtAuthenticator
import io.grpc.StatusException
import scalapb.zio_grpc.RequestContext
import zio.ZIO

object GrpcJwtAuth {

  import io.grpc.Metadata

  private val AuthHeader: Metadata.Key[String] =
    Metadata.Key.of(JwtAuthenticator.AuthHeader, Metadata.ASCII_STRING_MARSHALLER)

  def authenticated(authenticator: JwtAuthenticator): RequestContext => ZIO[Any, StatusException, String] =
    context =>
      for {
        maybeHeader <- context.metadata.get(AuthHeader)
        rawToken <- ZIO.getOrFailWith(new StatusException(io.grpc.Status.UNAUTHENTICATED))(maybeHeader)
        maybeSubject <- authenticator.authenticated(rawToken)
        subject <- ZIO.getOrFailWith(new StatusException(io.grpc.Status.UNAUTHENTICATED))(maybeSubject)
      } yield subject

}
