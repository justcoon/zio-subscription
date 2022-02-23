package com.jc.subscription.module

import io.getquill.context.zio.ZioJAsyncConnection
import zio.Has

package object db {
  type DbConnection = Has[ZioJAsyncConnection]
}
