package com.jc.subscription.module

import zio.Has

package object domain {

  type SubscriptionDomain = Has[SubscriptionDomain.Service]
}
