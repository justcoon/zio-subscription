package com.jc.subscription.module.db

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import io.getquill.context.zio.JAsyncContextConfig
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import zio.{Task, ZIO}

object DbInit {

  def run(config: JAsyncContextConfig[PostgreSQLConnection]): Task[MigrateResult] = {
    val cc = config.connectionPoolConfiguration
    val schema = Option(cc.getCurrentSchema).fold("public")(identity)
    val url = s"jdbc:postgresql://${cc.getHost}:${cc.getPort}/${cc.getDatabase}?currentSchema=$schema"
    ZIO.attempt(
      Flyway
        .configure()
        .validateMigrationNaming(true)
        .baselineOnMigrate(true)
        .dataSource(url, cc.getUsername, cc.getPassword)
        .load()
        .migrate()
    )
  }

  def run: ZIO[JAsyncContextConfig[PostgreSQLConnection], Throwable, MigrateResult] =
    ZIO.service[JAsyncContextConfig[PostgreSQLConnection]].flatMap(c => run(c))
}
