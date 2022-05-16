package com.jc.subscription.module

import com.jc.subscription.module.db.DbConnection

package object repo {
  type SubscriptionRepo = SubscriptionRepo.Service[DbConnection]
  type SubscriptionEventRepo = SubscriptionEventRepo.Service[DbConnection]
}
