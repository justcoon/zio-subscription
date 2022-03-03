package com.jc.subscription.model.config

import com.typesafe.config.Config
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.generic.semiauto._
import zio.config.magnolia.descriptor
import zio.{IO, Task, ZIO}
import zio.config.ReadError

sealed trait AppMode extends Product with Serializable

object AppMode {
  final case object all extends AppMode
  final case object svc extends AppMode
  final case object cdc extends AppMode

  implicit val modeReader: ConfigReader[AppMode] = deriveEnumerationReader[AppMode]

  def getMode(config: Config): Task[AppMode] = {
    ZIO.succeed(ConfigSource.fromConfig(config).at("mode").load[AppMode].getOrElse(AppMode.all))
  }

  implicit val configDescription = descriptor[AppMode].default(AppMode.all)

  def readMode(config: zio.config.ConfigSource): IO[ReadError[String], AppMode] = {
    import zio.config._
    import ConfigDescriptor._
    read(nested("mode")(configDescription) from config)
  }
}
