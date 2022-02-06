package com.jc.subscription.module.db

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.typesafe.config.ConfigFactory
import io.getquill.context.zio.{JAsyncContextConfig, PostgresJAsyncContextConfig, ZioJAsyncConnection}
import zio.{Has, ZIO, ZLayer}

object DbConnection {

  def getConfig(cfgNamespace: String = "db"): ZIO[Any, Nothing, JAsyncContextConfig[PostgreSQLConnection]] =
    ZIO.succeed(ConfigFactory.load()).map { config =>
      val c = if (config.hasPath(cfgNamespace)) {
        config.getConfig(cfgNamespace)
      } else {
        ConfigFactory.empty
      }
      PostgresJAsyncContextConfig(c)
    }

  val live: ZLayer[Any, Throwable, DbConnection] = getConfig().toLayer >>>
    ZioJAsyncConnection.live[PostgreSQLConnection]
}
