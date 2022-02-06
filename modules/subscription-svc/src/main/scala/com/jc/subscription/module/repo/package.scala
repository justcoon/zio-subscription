package com.jc.subscription.module

import zio.Has

package object repo {
  type SubscriptionRepo = Has[SubscriptionRepo.Service]
}
