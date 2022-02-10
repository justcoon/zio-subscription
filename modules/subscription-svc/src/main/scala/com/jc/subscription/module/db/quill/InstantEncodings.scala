package com.jc.subscription.module.db.quill

import io.getquill.MappedEncoding
import java.time.Instant
import java.util.Date

trait InstantEncodings {
  implicit val instantEncoding: MappedEncoding[Instant, Date] = MappedEncoding(Date.from)
  implicit val instantDecoding: MappedEncoding[Date, Instant] = MappedEncoding(_.toInstant)
}
