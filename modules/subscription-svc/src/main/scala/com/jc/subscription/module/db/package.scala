package com.jc.subscription.module

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.zio.{JAsyncContextConfig, ZioJAsyncConnection}
import zio.Has

package object db {
  type DbConfig = Has[JAsyncContextConfig[PostgreSQLConnection]]
  type DbConnection = Has[ZioJAsyncConnection]
}
