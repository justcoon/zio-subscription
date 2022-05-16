package com.jc.subscription.module

import io.getquill.context.zio.ZioJAsyncConnection

package object db {
  type DbConnection = ZioJAsyncConnection
}
