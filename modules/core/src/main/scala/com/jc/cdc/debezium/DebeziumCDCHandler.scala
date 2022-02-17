package com.jc.cdc.debezium

import io.circe
import io.debezium.config.Configuration
import io.debezium.engine.format.Json
import io.debezium.engine.{ChangeEvent, DebeziumEngine}
import zio.blocking.Blocking
import zio.{Chunk, Task, ZIO, ZManaged}
import com.jc.cdc.CDCHandler

import scala.util.{Failure, Success, Try}

object DebeziumCDCHandler {

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

  final private class DebeziumService(engine: DebeziumEngine[ChangeEvent[String, String]], blocking: Blocking.Service)
      extends CDCHandler.Service {

    override def start: Task[Unit] = {
      Task.effect(blocking.blockingExecutor.submitOrThrow(engine))
    }

    override def shutdown: Task[Unit] = {
      Task.effect(engine.close()).unit
    }

  }

  private def createChangeConsumer(handler: Chunk[ChangeEvent[String, String]] => Try[Unit]) = {
    new io.debezium.engine.DebeziumEngine.ChangeConsumer[ChangeEvent[String, String]] {
      override def handleBatch(
        records: java.util.List[ChangeEvent[String, String]],
        committer: DebeziumEngine.RecordCommitter[ChangeEvent[String, String]]): Unit = {
        import scala.jdk.CollectionConverters._
        val events = records.asScala
        handler(Chunk.fromIterable(events)) match {
          case Success(_) => committer.markBatchFinished()//events.foreach(committer.markProcessed)
          case Failure(e) => throw new InterruptedException(e.getMessage)
        }
      }
    }
  }

  private def createEngine(handler: Chunk[ChangeEvent[String, String]] => Try[Unit], config: Configuration) = {
    Try {
      val consumer = createChangeConsumer(handler)

      val engine: DebeziumEngine[ChangeEvent[String, String]] = DebeziumEngine
        .create(classOf[Json])
        .using(config.asProperties())
        .notifying(consumer)
        .build()

      engine
    }
  }

  def create[R](
    handler: Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit],
    config: Configuration): ZManaged[Blocking with R, Throwable, CDCHandler.Service] = {
    val engine = for {
      r <- ZIO.runtime[R]
      b: Blocking.Service <- ZIO.service[Blocking.Service]
      engine <- ZIO.fromTry {
        val h: Chunk[ChangeEvent[String, String]] => Try[Unit] =
          events => r.unsafeRunSync(handler(events)).toEither.toTry

        createEngine(h, config).map(engine => new DebeziumService(engine, b))
      }
    } yield engine
    engine.flatMap(s => s.start.as(s)).toManaged(s => s.shutdown.ignore)
  }

  def getChangeEventPayload[T](event: ChangeEvent[String, String])(implicit
    decoder: io.circe.Decoder[T]): Either[circe.Error, T] = {
    import io.circe.parser._
    parse(event.value()).flatMap { json =>
      json.hcursor.downField("payload").downField("after").as[T]
    }
  }

}
