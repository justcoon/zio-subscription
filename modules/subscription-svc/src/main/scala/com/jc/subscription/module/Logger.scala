package com.jc.subscription.module

import zio.{Runtime, ULayer}
import zio.logging.backend.SLF4J

object Logger {

  val layer: ULayer[Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j
}
