package com.jc.subscription.module.db

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.zio.ZioJAsyncConnection
import zio.ZLayer

object DbConnection {

  val live: ZLayer[DbConfig, Throwable, DbConnection] =
    ZioJAsyncConnection.live[PostgreSQLConnection]
}
