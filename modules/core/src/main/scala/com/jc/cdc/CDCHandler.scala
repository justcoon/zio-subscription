package com.jc.cdc

import com.jc.cdc.debezium.DebeziumCDC
import io.debezium.engine.{ChangeEvent, DebeziumEngine}
import zio.blocking.Blocking
import zio.{Chunk, ZIO, ZManaged}

object CDCHandler {

  def create[R, T](handler: Chunk[Either[Throwable, T]] => ZIO[R, Throwable, Unit])(implicit
    decoder: io.circe.Decoder[T]): ZManaged[Blocking with R, Throwable, DebeziumEngine[ChangeEvent[String, String]]] = {

    val typeHandler: Chunk[ChangeEvent[String, String]] => ZIO[R, Throwable, Unit] = events => {
      val typeEvents = events.map(DebeziumCDC.getChangeEventPayload[T])

      handler(typeEvents)
    }

    DebeziumCDC.create[R](typeHandler, DebeziumCDC.connectorConfiguration())
  }

}
