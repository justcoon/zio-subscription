package com.jc.subscription.module.db.cdc

import com.jc.cdc.CdcHandler
import com.jc.subscription.model.config.DbCdcConfig
import com.jc.subscription.module.repo.SubscriptionEventRepo
import io.debezium.config.Configuration
import zio.{Chunk, Has, ZIO, ZLayer}
import zio.blocking.Blocking

object PostgresCdc {

  // https://debezium.io/documentation/reference/1.8/development/engine.html#_in_the_code
  def getConfig(dbCdcConfig: DbCdcConfig): Configuration = {
    val poolConfig = dbCdcConfig.connection.connectionPoolConfiguration
    val tables = "subscription_events" :: Nil
    val schema = "public"
    val tablesInclude = tables.map(table => s"$schema.$table").mkString(",")
    val offsetStoreFilename = s"${dbCdcConfig.cdc.offsetStoreDir}/${poolConfig.getDatabase}-offsets.dat"
    val dbHistoryFilename = s"${dbCdcConfig.cdc.offsetStoreDir}/${poolConfig.getDatabase}-dbhistory.dat"
    val slotName = s"${poolConfig.getDatabase}_subscription"
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
      .`with`("slot.name", slotName)
      .build
  }

  def create[R](
    dbCdcConfig: DbCdcConfig,
    handler: Chunk[Either[Throwable, SubscriptionEventRepo.SubscriptionEvent]] => ZIO[R, Throwable, Unit])
    : ZLayer[Blocking with R, Throwable, CdcHandler] = {
    val typeHandler = CdcHandler.postgresTypeHandler(handler)(SubscriptionEventRepo.SubscriptionEvent.cdcDecoder)
    val configLayer = ZLayer.succeed(getConfig(dbCdcConfig))
    val cdc = CdcHandler.create(typeHandler).provideSomeLayer[Blocking with R](configLayer)
    cdc.toLayer
  }

  def create[R](handler: Chunk[Either[Throwable, SubscriptionEventRepo.SubscriptionEvent]] => ZIO[R, Throwable, Unit])
    : ZLayer[Has[DbCdcConfig] with Blocking with R, Throwable, CdcHandler] = {
    val typeHandler = CdcHandler.postgresTypeHandler(handler)(SubscriptionEventRepo.SubscriptionEvent.cdcDecoder)
    val configLayer = ZIO.service[DbCdcConfig].map(getConfig).toLayer
    val cdc = CdcHandler.create(typeHandler).provideSomeLayer[Has[DbCdcConfig] with Blocking with R](configLayer)
    cdc.toLayer
  }
}
