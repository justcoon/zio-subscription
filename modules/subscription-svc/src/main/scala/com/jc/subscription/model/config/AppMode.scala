package com.jc.subscription.model.config

import com.typesafe.config.Config
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.generic.semiauto._
import zio.{Task, ZIO}

sealed trait AppMode extends Product with Serializable

object AppMode {
  final case object All extends AppMode
  final case object Svc extends AppMode
  final case object Cdc extends AppMode

  implicit val modeReader: ConfigReader[AppMode] = deriveEnumerationReader[AppMode]

  def getMode(config: Config): Task[AppMode] = {
    ZIO.succeed(ConfigSource.fromConfig(config).at("mode").load[AppMode].getOrElse(AppMode.All))
  }
}
