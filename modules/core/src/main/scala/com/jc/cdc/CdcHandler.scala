package com.jc.cdc

import com.jc.cdc.debezium.DebeziumCdcHandler
import io.debezium.config.Configuration
import io.debezium.engine.ChangeEvent
import zio.blocking.Blocking
import zio.{Chunk, Has, Task, ZIO, ZManaged}

object CdcHandler {

  trait Service {
    def start: Task[Unit]
    def shutdown: Task[Unit]
  }

  def postgresTypeHandler[R, T](handler: Chunk[Either[Throwable, T]] => ZIO[R, Throwable, Unit])(implicit
    decoder: io.circe.Decoder[T]): Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit] = { events =>
    val typeEvents = events.map(DebeziumCdcHandler.getPostgresChangeEventPayload[T])
    handler(typeEvents)
  }

  def create[R](handler: Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit])
    : ZManaged[Has[Configuration] with Blocking with R, Throwable, Service] = {
    DebeziumCdcHandler.create[R](handler)
  }

}
