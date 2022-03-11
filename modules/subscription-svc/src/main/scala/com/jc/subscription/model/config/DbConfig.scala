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

  private val jacConfigDescription: ConfigDescriptor[JAsyncContextConfig[PostgreSQLConnection]] = ConfigDescriptor
    .map[String](ConfigDescriptor.string)
    .transform[JAsyncContextConfig[PostgreSQLConnection]](
      { cfg =>
        val config = ConfigFactory.parseMap(cfg.asJava)
        PostgresJAsyncContextConfig(config)
      },
      {
        case cfg: PostgresJAsyncContextConfig =>
          cfg.config.entrySet().asScala.map(e => e.getKey -> e.getValue.toString).toMap
        case _ => Map.empty
      }
    )

  implicit val jacDescription = Descriptor[JAsyncContextConfig[PostgreSQLConnection]](jacConfigDescription, true)

  implicit val dbConfigDescription: ConfigDescriptor[DbConfig] = descriptor[DbConfig].mapKey(toKebabCase)
}

final case class CdcConfig(offsetStoreDir: OffsetDir)

object CdcConfig {

  implicit val cdcConfigDescription: ConfigDescriptor[CdcConfig] = descriptor[CdcConfig].mapKey(toKebabCase)
}

final case class DbCdcConfig(cdc: CdcConfig, connection: JAsyncContextConfig[PostgreSQLConnection])

object DbCdcConfig {
  import DbConfig.jacDescription
  implicit val dbCdcConfigDescription: ConfigDescriptor[DbCdcConfig] = descriptor[DbCdcConfig].mapKey(toKebabCase)
}
