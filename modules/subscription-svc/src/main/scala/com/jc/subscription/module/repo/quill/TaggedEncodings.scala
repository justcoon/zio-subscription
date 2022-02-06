package com.jc.subscription.module.repo.quill

import io.getquill.MappedEncoding
import shapeless.tag
import shapeless.tag.@@

trait TaggedEncodings {

  implicit def taggedStringEncoding[T]: MappedEncoding[String @@ T, String] = MappedEncoding(identity)

  implicit def taggedStringDecoding[T]: MappedEncoding[String, String @@ T] = MappedEncoding(s => tag[T][String](s))
}
