package com.jc.subscription.model.config

import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.typesafe.config.ConfigFactory
import io.getquill.context.zio.{JAsyncContextConfig, PostgresJAsyncContextConfig}
import zio.Config
import zio.config.refined._
import zio.config.magnolia.{deriveConfig, DeriveConfig}

sealed trait DbConfig {
  def connection: JAsyncContextConfig[PostgreSQLConnection]
}

object DbConfig {

  import scala.jdk.CollectionConverters._

  val jacConfig: Config[JAsyncContextConfig[PostgreSQLConnection]] = Config
    .table(Config.string)
    .mapAttempt[JAsyncContextConfig[PostgreSQLConnection]] { cfg =>
      val config = ConfigFactory.parseMap(cfg.asJava)
      PostgresJAsyncContextConfig(config)
    }

  implicit val jacDerivedConfig = DeriveConfig[JAsyncContextConfig[PostgreSQLConnection]](jacConfig, true)
}

final case class DbConConfig(connection: JAsyncContextConfig[PostgreSQLConnection]) extends DbConfig

object DbConConfig {
  import DbConfig.jacDerivedConfig
  implicit val dbConfig: Config[DbConConfig] = deriveConfig[DbConConfig]
}

final case class CdcConfig(offsetStoreDir: OffsetDir)

object CdcConfig {
  implicit val cdcConfig: Config[CdcConfig] = deriveConfig[CdcConfig]
}

final case class DbCdcConfig(cdc: CdcConfig, connection: JAsyncContextConfig[PostgreSQLConnection]) extends DbConfig

object DbCdcConfig {
  import DbConfig.jacDerivedConfig
  implicit val dbCdcConfig: Config[DbCdcConfig] = deriveConfig[DbCdcConfig]
}
