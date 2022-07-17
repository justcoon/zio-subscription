package com.jc.subscription.module

import zio.{Runtime, Trace, ULayer}
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object Logger {

  def getLoggerName(trace: Trace): String =
    trace match {
      case Trace(location, _, _) =>
        val last = location.lastIndexOf(".")
        if (last > 0) {
          location.substring(0, last)
        } else location
      case _ => "zio-subscription-logger"
    }

  val layer: ULayer[Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j(
    zio.LogLevel.Debug,
    LogFormat.line |-| LogFormat.cause,
    getLoggerName
  )
}
