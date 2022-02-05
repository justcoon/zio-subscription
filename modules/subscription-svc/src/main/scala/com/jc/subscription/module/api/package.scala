package com.jc.subscription.module

import com.jc.subscription.domain.proto.ZioSubscription.RCSubscriptionApiService
import zio.Has

package object api {

  type SubscriptionGrpcApiHandler = Has[RCSubscriptionApiService[Any]]
}
