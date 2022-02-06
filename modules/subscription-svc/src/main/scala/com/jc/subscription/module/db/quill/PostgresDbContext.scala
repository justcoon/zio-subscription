package com.jc.subscription.module.db.quill

import io.getquill.SnakeCase
import io.getquill.context.zio.PostgresZioJAsyncContext

object PostgresDbContext extends PostgresZioJAsyncContext(SnakeCase)
