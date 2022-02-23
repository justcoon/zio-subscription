package com.jc.subscription.module.db.cdc

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.jc.cdc.CDCHandler
import com.jc.subscription.model.config.DbConfig
import com.jc.subscription.module.repo.SubscriptionEventRepo
import io.debezium.config.Configuration
import zio.{Chunk, ZIO, ZLayer}
import zio.blocking.Blocking

object PostgresCDC {

  def getConfig(dbConfig: DbConfig): Configuration = {
    val poolConfig = dbConfig.connection.connectionPoolConfiguration
    val tables = "subscription_events" :: Nil
    val schema = "public"
    val tablesInclude = tables.map(table => s"$schema.$table").mkString(",")
    val offsetStoreFilename = s"${dbConfig.cdc.offsetStoreDir}/subscription-offsets.dat"
    val dbHistoryFilename = s"${dbConfig.cdc.offsetStoreDir}/subscription-dbhistory.dat"
    Configuration.create
      .`with`("name", "subscription-outbox-connector")
      .`with`("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
      .`with`("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
      .`with`("offset.storage.file.filename", offsetStoreFilename)
      .`with`("offset.flush.interval.ms", "5000")
      .`with`("database.hostname", poolConfig.getHost)
      .`with`("plugin.name", "pgoutput")
      .`with`("database.port", poolConfig.getPort)
      .`with`("database.user", poolConfig.getUsername)
      .`with`("database.password", poolConfig.getPassword)
      .`with`("database.dbname", poolConfig.getDatabase)
      .`with`("table.include.list", tablesInclude)
      .`with`("database.server.id", "1")
      .`with`("database.server.name", "dbserver")
      .`with`("database.history", "io.debezium.relational.history.FileDatabaseHistory")
      .`with`("database.history.file.filename", dbHistoryFilename)
      .build
  }

  def create[R](
    dbConfig: DbConfig,
    handler: Chunk[Either[Throwable, SubscriptionEventRepo.SubscriptionEvent]] => ZIO[R, Throwable, Unit])
    : ZLayer[Blocking with R, Throwable, CDCHandler] = {

    val configLayer = ZLayer.succeed(getConfig(dbConfig))

    val cdc = CDCHandler
      .create(handler)(SubscriptionEventRepo.SubscriptionEvent.cdcDecoder)
      .provideSomeLayer[Blocking with R](configLayer)

    cdc.toLayer
  }
}
