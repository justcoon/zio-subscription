package com.jc.subscription.module

import com.jc.subscription.module.db.DbConnection
import zio.Has

package object repo {
  type SubscriptionRepo = Has[SubscriptionRepo.Service[DbConnection]]
  type SubscriptionEventRepo = Has[SubscriptionEventRepo.Service[DbConnection]]
}
