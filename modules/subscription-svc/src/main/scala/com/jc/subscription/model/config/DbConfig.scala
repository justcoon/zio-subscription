package com.jc.subscription.model.config

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.typesafe.config.ConfigFactory
import io.getquill.context.zio.{JAsyncContextConfig, PostgresJAsyncContextConfig}
import zio.config.refined._
import zio.config.magnolia.{descriptor, Descriptor}
import zio.config.toKebabCase
import zio.config.ConfigDescriptor

final case class DbConfig(connection: JAsyncContextConfig[PostgreSQLConnection])

object DbConfig {

  import scala.jdk.CollectionConverters._

  private val cd: ConfigDescriptor[JAsyncContextConfig[PostgreSQLConnection]] = ConfigDescriptor
    .map[String](ConfigDescriptor.string)
    .transform[JAsyncContextConfig[PostgreSQLConnection]](
      cfg => PostgresJAsyncContextConfig(ConfigFactory.parseMap(cfg.asJava)),
      {
        case cfg: PostgresJAsyncContextConfig =>
          cfg.config.entrySet().asScala.map(e => e.getKey -> e.getValue.toString).toMap
        case _ => Map.empty
      }
    )

  implicit val jacConfigDescription = Descriptor[JAsyncContextConfig[PostgreSQLConnection]](cd, true)

  implicit val dbConfigDescription = descriptor[DbConfig].mapKey(toKebabCase)
}

final case class CdcConfig(offsetStoreDir: OffsetDir)

object CdcConfig {

  implicit val cdcConfigDescription: _root_.zio.config.ConfigDescriptor[CdcConfig] =
    descriptor[CdcConfig].mapKey(toKebabCase)
}

final case class DbCdcConfig(cdc: CdcConfig, connection: JAsyncContextConfig[PostgreSQLConnection])

object DbCdcConfig {
  import DbConfig.jacConfigDescription
  implicit val dbCdcConfigDescription = descriptor[DbCdcConfig].mapKey(toKebabCase)
}
