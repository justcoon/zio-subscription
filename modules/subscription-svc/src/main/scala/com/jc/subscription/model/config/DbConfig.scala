package com.jc.subscription.model.config

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.zio.{JAsyncContextConfig, PostgresJAsyncContextConfig}
import pureconfig.ConfigReader.Result
import pureconfig.{ConfigCursor, ConfigReader}
import pureconfig.generic.semiauto.deriveReader

final case class DbConfig(connection: JAsyncContextConfig[PostgreSQLConnection])

object DbConfig {

  implicit lazy val jAsyncContextConfigReader = new ConfigReader[JAsyncContextConfig[PostgreSQLConnection]] {

    override def from(cur: ConfigCursor): Result[JAsyncContextConfig[PostgreSQLConnection]] = {
      cur.asObjectCursor.map { c =>
        PostgresJAsyncContextConfig(c.objValue.toConfig)
      }
    }
  }

  implicit lazy val configReader = deriveReader[DbConfig]
}
