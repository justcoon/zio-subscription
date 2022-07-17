package com.jc.subscription.module.db

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.zio.{JAsyncContextConfig, ZioJAsyncConnection}
import zio.ZLayer

object DbConnection {

  def make(config: JAsyncContextConfig[PostgreSQLConnection]): ZLayer[Any, Throwable, DbConnection] =
    ZLayer.succeed(config) >>> layer

  val layer: ZLayer[JAsyncContextConfig[PostgreSQLConnection], Throwable, DbConnection] =
    ZioJAsyncConnection.live[PostgreSQLConnection]
}
