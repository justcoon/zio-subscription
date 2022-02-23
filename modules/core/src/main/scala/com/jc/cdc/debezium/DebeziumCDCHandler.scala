package com.jc.cdc.debezium

import io.circe
import io.debezium.config.Configuration
import io.debezium.engine.format.Json
import io.debezium.engine.{ChangeEvent, DebeziumEngine}
import zio.blocking.Blocking
import zio.{Chunk, Has, Task, ZIO, ZManaged}
import com.jc.cdc.CDCHandler

import scala.util.{Failure, Success, Try}

object DebeziumCDCHandler {

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
          case Success(_) =>
            events.foreach(committer.markProcessed)
            committer.markBatchFinished()
          case Failure(e) => throw new InterruptedException(e.getMessage)
        }
      }
    }
  }

  // https://debezium.io/documentation/reference/1.8/development/engine.html#_in_the_code
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
    handler: Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit]
  ): ZManaged[Has[Configuration] with Blocking with R, Throwable, CDCHandler.Service] = {
    val engine = for {
      r <- ZIO.runtime[R]
      c <- ZIO.service[Configuration]
      b <- ZIO.service[Blocking.Service]
      engine <- ZIO.fromTry {
        val h: Chunk[ChangeEvent[String, String]] => Try[Unit] =
          events => r.unsafeRunSync(handler(events)).toEither.toTry

        createEngine(h, c).map(engine => new DebeziumService(engine, b))
      }
    } yield engine
    engine.flatMap(s => s.start.as(s)).toManaged(s => s.shutdown.ignore)
  }

  // https://debezium.io/documentation/reference/1.8/connectors/postgresql.html#postgresql-update-events
  def getPostgresChangeEventPayload[T](event: ChangeEvent[String, String])(implicit
    decoder: io.circe.Decoder[T]): Either[circe.Error, T] = {
    import io.circe.parser._
    parse(event.value()).flatMap { json =>
      json.hcursor.downField("payload").downField("after").as[T]
    }
  }

}
