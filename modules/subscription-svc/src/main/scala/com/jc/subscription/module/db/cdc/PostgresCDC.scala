package com.jc.subscription.module.db.cdc

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.jc.cdc.CDCHandler
import com.jc.subscription.module.db.DbConfig
import com.jc.subscription.module.repo.SubscriptionEventRepo
import io.debezium.config.Configuration
import io.getquill.context.zio.JAsyncContextConfig
import zio.{Chunk, Has, ZIO, ZLayer, ZManaged}
import zio.blocking.Blocking

object PostgresCDC {

  def getConfig(connectionConfig: JAsyncContextConfig[PostgreSQLConnection]): Configuration = {
    val poolConfig = connectionConfig.connectionPoolConfiguration
    val tables = "subscription_events" :: Nil
    val schema = "public"
    val tablesInclude = tables.map(table => s"$schema.$table").mkString(",")

    Configuration.create
      .`with`("name", "subscription-outbox-connector")
      .`with`("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
      .`with`("offset.storage.file.filename", "/tmp/offsets.dat")
      .`with`("offset.flush.interval.ms", "60000")
      .`with`("database.hostname", poolConfig.getHost)
      .`with`("plugin.name", "pgoutput")
      .`with`("database.port", poolConfig.getPort)
      .`with`("database.user", poolConfig.getUsername)
      .`with`("database.password", poolConfig.getPassword)
      .`with`("database.dbname", poolConfig.getDatabase)
      .`with`("table.include.list", tablesInclude)
      .`with`("database.server.id", "1")
      .`with`("database.server.name", "dbserver")
      .build
  }

  def create[R <: Has[_]](
    handler: Chunk[Either[Throwable, SubscriptionEventRepo.SubscriptionEvent]] => ZIO[R, Throwable, Unit])
    : ZLayer[DbConfig with Blocking with R, Throwable, CDCHandler] = {

    val cdc = for {
      dbConfig <- ZManaged.service[JAsyncContextConfig[PostgreSQLConnection]]
      configLayer = ZLayer.succeed(getConfig(dbConfig))
      cdc <- CDCHandler
        .create(handler)(SubscriptionEventRepo.SubscriptionEvent.cdcDecoder)
        .provideSomeLayer[Blocking with R](configLayer)
    } yield cdc

    cdc.toLayer
  }
}
