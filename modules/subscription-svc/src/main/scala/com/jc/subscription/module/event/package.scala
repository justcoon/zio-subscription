package com.jc.subscription.module

import zio.Has

package object event {
  type SubscriptionEventProducer = Has[SubscriptionEventProducer.Service]
}
