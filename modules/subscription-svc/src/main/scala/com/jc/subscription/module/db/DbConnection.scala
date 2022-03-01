package com.jc.subscription.module.db

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.zio.{JAsyncContextConfig, ZioJAsyncConnection}
import zio.{Has, ZLayer}

object DbConnection {

  def create(config: JAsyncContextConfig[PostgreSQLConnection]): ZLayer[Any, Throwable, DbConnection] =
    ZLayer.succeed(config) >>> live

  val live: ZLayer[Has[JAsyncContextConfig[PostgreSQLConnection]], Throwable, DbConnection] =
    ZioJAsyncConnection.live[PostgreSQLConnection]
}
