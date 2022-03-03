package com.jc.subscription.model.config

import zio.config.magnolia.descriptor
import zio.IO
import zio.config.ReadError

sealed trait AppMode extends Product with Serializable

object AppMode {
  final case object all extends AppMode
  final case object svc extends AppMode
  final case object cdc extends AppMode

  implicit val configDescription = descriptor[AppMode].default(AppMode.all)

  def readMode(config: zio.config.ConfigSource): IO[ReadError[String], AppMode] = {
    import zio.config._
    import ConfigDescriptor._
    read(nested("mode")(configDescription) from config)
  }
}
