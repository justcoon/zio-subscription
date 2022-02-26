package com.jc.subscription.model.config

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.getquill.context.zio.{JAsyncContextConfig, PostgresJAsyncContextConfig}
import pureconfig.ConfigReader.Result
import pureconfig.{ConfigCursor, ConfigReader}
import pureconfig.generic.semiauto.deriveReader

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
}

final case class CdcConfig(offsetStoreDir: String Refined NonEmpty)

object CdcConfig {
  import eu.timepit.refined.pureconfig._
  implicit lazy val configReader = deriveReader[CdcConfig]
}

final case class DbCdcConfig(cdc: CdcConfig, connection: JAsyncContextConfig[PostgreSQLConnection])

object DbCdcConfig {
  import DbConfig.jAsyncContextConfigReader

  implicit lazy val configReader = deriveReader[DbCdcConfig]
}
