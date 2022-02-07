package com.jc.cdc

import io.debezium.config.Configuration
import io.debezium.engine.format.{ChangeEventFormat, Json}
import io.debezium.engine.{ChangeEvent, DebeziumEngine}
import zio.blocking.{blocking, Blocking}
import zio.{ZIO, ZManaged}

import java.util.concurrent.Executor
import java.util.function.Consumer
import scala.util.Try

object DebeziumCDC {

  def connectorConfiguration(): Configuration = io.debezium.config.Configuration.create
    .`with`("name", "subscription-outbox-connector")
    .`with`("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
    .`with`("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
    .`with`("offset.storage.file.filename", "/tmp/offsets.dat")
    .`with`("offset.flush.interval.ms", "60000")
    .`with`("database.hostname", "localhost")
    .`with`("plugin.name", "pgoutput")
    .`with`("database.port", "5432")
    .`with`("database.user", "postgres")
    .`with`("database.password", "1234")
    .`with`("database.dbname", "subscription")
//    .`with`("database.include.list", "subscription")
//    .`with`("schema.whitelist", "public")
    .`with`("table.whitelist", "public.subscriptions")
//    .`with`("include.schema.changes", "false")
    .`with`("database.server.id", "1")
    .`with`("database.server.name", "dbserver")
    .`with`("database.history", "io.debezium.relational.history.FileDatabaseHistory")
    .`with`("database.history.file.filename", "/tmp/dbhistory.dat")
    .build

  def createEngine(handler: ChangeEvent[String, String] => Unit) = {
    Try {
      val c: java.util.function.Consumer[ChangeEvent[String, String]] = (t: ChangeEvent[String, String]) => handler(t)

      val e: DebeziumEngine[ChangeEvent[String, String]] = DebeziumEngine
        .create(classOf[Json])
        .using(connectorConfiguration().asProperties())
        .notifying(c)
        .build()

      e
    }
  }

  def create(handler: ChangeEvent[String, String] => Unit)
    : ZManaged[Blocking, Throwable, DebeziumEngine[ChangeEvent[String, String]]] = {
    val engine = for {
      b <- ZIO.service[Blocking.Service]
      engine <- ZIO.fromTry(createEngine(handler))
    } yield {
      b.blockingExecutor.asJava.execute(engine)
      engine
    }
    engine.toManaged(engine => ZIO.effect(engine.close()).ignore)
  }

}
