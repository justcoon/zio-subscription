package com.jc

import zio.Has

package object cdc {
  type CDCHandler = Has[CDCHandler.Service]
}
