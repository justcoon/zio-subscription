package com.jc.subscription.module.db

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.zio.JAsyncContextConfig
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import zio.{Has, Task, ZIO}

import scala.util.Try

object DbInit {

  def run(config: JAsyncContextConfig[PostgreSQLConnection]): Task[MigrateResult] = {
    val cc = config.connectionPoolConfiguration
    val url = s"jdbc:postgresql://${cc.getHost}:${cc.getPort}/${cc.getDatabase}"
    ZIO.fromTry(Try {
      Flyway
        .configure()
        .validateMigrationNaming(true)
        .baselineOnMigrate(true)
        .dataSource(url, cc.getUsername, cc.getPassword)
        .load()
        .migrate()
    })
  }

  def run: ZIO[Has[JAsyncContextConfig[PostgreSQLConnection]], Throwable, MigrateResult] =
    ZIO.service[JAsyncContextConfig[PostgreSQLConnection]].flatMap(c => run(c))
}
