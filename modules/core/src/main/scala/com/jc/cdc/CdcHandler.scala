package com.jc.cdc

import com.jc.cdc.debezium.DebeziumCdcHandler
import io.debezium.config.Configuration
import io.debezium.engine.ChangeEvent
import zio.{Chunk, Scope, Task, ZIO}

trait CdcHandler {
  def start: Task[Unit]
  def shutdown: Task[Unit]
}

object CdcHandler {

  def postgresTypeHandler[R, T](handler: Chunk[Either[Throwable, T]] => ZIO[R, Throwable, Unit])(implicit
    decoder: io.circe.Decoder[T]): Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit] = { events =>
    val typeEvents = events.map(DebeziumCdcHandler.getPostgresChangeEventPayload[T])
    handler(typeEvents)
  }

  def make[R](handler: Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit])
    : ZIO[Configuration with Scope with R, Throwable, CdcHandler] = {
    DebeziumCdcHandler.make[R](handler)
  }

}
