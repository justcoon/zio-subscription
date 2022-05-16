package com.jc.subscription.module

import com.jc.subscription.domain.proto.ZioSubscriptionApi.RCSubscriptionApiService

package object api {

  type SubscriptionGrpcApiHandler = RCSubscriptionApiService[Any]
}
