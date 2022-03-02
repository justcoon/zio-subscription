package com.jc.subscription.model.config

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.typesafe.config.ConfigFactory
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.getquill.context.zio.{JAsyncContextConfig, PostgresJAsyncContextConfig}
import pureconfig.ConfigReader.Result
import pureconfig.{ConfigCursor, ConfigReader}
import pureconfig.generic.semiauto.deriveReader
import zio.config.refined._
import zio.config.magnolia.descriptor
import zio.config.magnolia.Descriptor
import zio.config.toKebabCase

final case class DbConfig(connection: JAsyncContextConfig[PostgreSQLConnection])

object DbConfig {

  implicit lazy val jAsyncContextConfigReader = new ConfigReader[JAsyncContextConfig[PostgreSQLConnection]] {

    override def from(cur: ConfigCursor): Result[JAsyncContextConfig[PostgreSQLConnection]] = {
      cur.asObjectCursor.map { c =>
        PostgresJAsyncContextConfig(c.objValue.toConfig)
      }
    }
  }

  implicit lazy val configReader = deriveReader[DbConfig]

  implicit val jacConfigDescription: Descriptor[JAsyncContextConfig[PostgreSQLConnection]] =
    Descriptor[String].transform(cfg => PostgresJAsyncContextConfig(ConfigFactory.parseString(cfg)), _.toString)
  implicit val dbConfigDescription = descriptor[DbConfig].mapKey(toKebabCase)
}

final case class CdcConfig(offsetStoreDir: String Refined NonEmpty)

object CdcConfig {
  import eu.timepit.refined.pureconfig._
  implicit lazy val configReader = deriveReader[CdcConfig]

  implicit val cdcConfigDescription = descriptor[CdcConfig].mapKey(toKebabCase)
}

final case class DbCdcConfig(cdc: CdcConfig, connection: JAsyncContextConfig[PostgreSQLConnection])

object DbCdcConfig {
  import DbConfig.jAsyncContextConfigReader
  implicit lazy val configReader = deriveReader[DbCdcConfig]
  import DbConfig.jacConfigDescription
  implicit val dbCdcConfigDescription = descriptor[DbCdcConfig].mapKey(toKebabCase)
}
