package com.jc.cdc

import com.jc.cdc.debezium.DebeziumCDCHandler
import io.debezium.config.Configuration
import io.debezium.engine.ChangeEvent
import zio.blocking.Blocking
import zio.{Chunk, Has, Task, ZIO, ZManaged}

object CDCHandler {

  trait Service {
    def start: Task[Unit]
    def shutdown: Task[Unit]
  }

  def typeHandler[R, T](handler: Chunk[Either[Throwable, T]] => ZIO[R, Throwable, Unit])(implicit
    decoder: io.circe.Decoder[T]): Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit] = { events =>
    val typeEvents = events.map(DebeziumCDCHandler.getChangeEventPayload[T])
    handler(typeEvents)
  }

  def create[R, T](handler: Chunk[Either[Throwable, T]] => ZIO[R, Throwable, Unit])(implicit
    decoder: io.circe.Decoder[T]): ZManaged[Has[Configuration] with Blocking with R, Throwable, Service] = {
    DebeziumCDCHandler.create[R](typeHandler(handler))
  }

}
