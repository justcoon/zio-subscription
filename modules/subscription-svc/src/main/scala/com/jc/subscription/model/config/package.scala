package com.jc.subscription.model

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.IPv4

package object config {
  type Addresses = List[String] Refined NonEmpty
  type IpAddress = String Refined IPv4
  type TopicName = String Refined NonEmpty
  type IndexName = String Refined NonEmpty
  type OffsetDir = String Refined NonEmpty
}
