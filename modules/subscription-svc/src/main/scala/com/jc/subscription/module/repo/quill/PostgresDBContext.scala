package com.jc.subscription.module.repo.quill

import io.getquill.SnakeCase
import io.getquill.context.zio.PostgresZioJAsyncContext

object PostgresDBContext extends PostgresZioJAsyncContext(SnakeCase)
