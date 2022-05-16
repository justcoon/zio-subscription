package com.jc.logging

import com.jc.logging.proto.ZioLoggingSystemApi.RCLoggingSystemApiService

package object api {
  type LoggingSystemGrpcApiHandler = RCLoggingSystemApiService[Any]
}
