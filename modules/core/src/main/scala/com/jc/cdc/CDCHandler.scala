package com.jc.cdc

import com.jc.cdc.debezium.DebeziumCDCHandler
import io.debezium.engine.ChangeEvent
import zio.blocking.Blocking
import zio.{Chunk, Task, ZIO, ZManaged}

object CDCHandler {

  trait Service {
    def start: Task[Unit]
    def shutdown: Task[Unit]
  }

  def create[R, T](handler: Chunk[Either[Throwable, T]] => ZIO[R, Throwable, Unit])(implicit
    decoder: io.circe.Decoder[T]): ZManaged[Blocking with R, Throwable, Service] = {

    val typeHandler: Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit] = events => {
      val typeEvents = events.map(DebeziumCDCHandler.getChangeEventPayload[T])

      handler(typeEvents)
    }

    DebeziumCDCHandler.create[R](typeHandler, DebeziumCDCHandler.connectorConfiguration())
  }

}
