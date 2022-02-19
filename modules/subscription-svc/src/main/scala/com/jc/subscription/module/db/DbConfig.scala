package com.jc.subscription.module.db

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.typesafe.config.ConfigFactory
import io.getquill.context.zio.{JAsyncContextConfig, PostgresJAsyncContextConfig}
import zio.{ZIO, ZLayer}

object DbConfig {

  def getConfig(cfgNamespace: String): ZIO[Any, Nothing, JAsyncContextConfig[PostgreSQLConnection]] =
    ZIO.succeed(ConfigFactory.load()).map { config =>
      val c = if (config.hasPath(cfgNamespace)) {
        config.getConfig(cfgNamespace)
      } else {
        ConfigFactory.empty
      }
      PostgresJAsyncContextConfig(c)
    }

  def create(cfgNamespace: String = "db"): ZLayer[Any, Nothing, DbConfig] =
    getConfig(cfgNamespace).toLayer
}
