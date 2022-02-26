package com.jc

import zio.Has

package object cdc {
  type CdcHandler = Has[CdcHandler.Service]
}
