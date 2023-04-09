package com.jc.subscription.model.config

import zio.config.magnolia.deriveConfig
import zio.ZIO

sealed trait AppMode extends Product with Serializable

object AppMode {
  final case object all extends AppMode
  final case object svc extends AppMode
  final case object cdc extends AppMode

  implicit val config = deriveConfig[AppMode].withDefault(AppMode.all)

  def readMode = {
    ZIO.config(config.nested("mode"))
  }
}
