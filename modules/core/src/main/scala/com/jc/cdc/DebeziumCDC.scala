package com.jc.cdc

import io.debezium.config.Configuration
import io.debezium.engine.format.Json
import io.debezium.engine.{ChangeEvent, DebeziumEngine}
import zio.blocking.Blocking
import zio.{Chunk, URIO, ZIO, ZManaged}

import scala.util.{Failure, Success, Try}

object DebeziumCDC {

  def connectorConfiguration(): Configuration = Configuration.create
    .`with`("name", "subscription-outbox-connector")
    .`with`("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
//    .`with`("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
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
    .`with`("table.include.list", "public.subscription_events")
//    .`with`("include.schema.changes", "false")
    .`with`("database.server.id", "1")
    .`with`("database.server.name", "dbserver")
    .`with`("database.history", "io.debezium.relational.history.FileDatabaseHistory")
    .`with`("database.history.file.filename", "/tmp/dbhistory.dat")
    .build

  def getChangeEventPayload(event: ChangeEvent[String, String]): Option[io.circe.Json] = {
    import io.circe.parser._
    parse(event.value()).toOption.flatMap { json =>
      json.hcursor.downField("payload").downField("after").focus
    }
  }

  def create[R](handler: Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit])
    : ZManaged[Blocking with R, Throwable, DebeziumEngine[ChangeEvent[String, String]]] = {
    val engine = for {
      r <- ZIO.runtime[R]
      b <- ZIO.service[Blocking.Service]
      engine <- ZIO.fromTry {
        createEngine { events =>
          r.unsafeRunSync(handler(events)).toEither.toTry
        }
      }
    } yield {
      b.blockingExecutor.submitOrThrow(engine)
      engine
    }
    engine.toManaged(engine => ZIO.effect(engine.close()).ignore)
  }

  def createChangeConsumer(handler: Chunk[ChangeEvent[String, String]] => Try[Unit]) = {
    new io.debezium.engine.DebeziumEngine.ChangeConsumer[ChangeEvent[String, String]] {
      override def handleBatch(
        records: java.util.List[ChangeEvent[String, String]],
        committer: DebeziumEngine.RecordCommitter[ChangeEvent[String, String]]): Unit = {
        import scala.jdk.CollectionConverters._
        val events = records.asScala
        handler(Chunk.fromIterable(events)) match {
          case Success(_) => events.foreach(committer.markProcessed)
          case Failure(e) => throw new InterruptedException(e.getMessage)
        }
      }
    }
  }

  def createEngine(handler: Chunk[ChangeEvent[String, String]] => Try[Unit]) = {
    Try {
      val consumer = createChangeConsumer(handler)
      val config = connectorConfiguration()

      val engine: DebeziumEngine[ChangeEvent[String, String]] = DebeziumEngine
        .create(classOf[Json])
        .using(config.asProperties())
        .notifying(consumer)
        .build()

      engine
    }
  }
}
