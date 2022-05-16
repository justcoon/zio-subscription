package com.jc.subscription.module.db

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.zio.{JAsyncContextConfig, ZioJAsyncConnection}
import zio.ZLayer

object DbConnection {

  def create(config: JAsyncContextConfig[PostgreSQLConnection]): ZLayer[Any, Throwable, DbConnection] =
    ZLayer.succeed(config) >>> live

  val live: ZLayer[JAsyncContextConfig[PostgreSQLConnection], Throwable, DbConnection] =
    ZioJAsyncConnection.live[PostgreSQLConnection]
}
