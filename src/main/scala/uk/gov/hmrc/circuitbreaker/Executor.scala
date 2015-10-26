/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.circuitbreaker

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

private[circuitbreaker] trait CircuitBreakerExecutor {

  self: CircuitBreakerModel =>

  def invoke[T](f: => Future[T]): Future[T] = {
    try {currentState.stateAwareInvoke(f).map
    {x => currentState.processCallResult(wasCallSuccessful = true); x}
      .recoverWith(processError())} catch processError()
  }

  private def processError[T](): PartialFunction[Throwable, Future[T]] = PartialFunction[Throwable, Future[T]] {
    throwable: Throwable =>
      throwable match {
        case unhealthyService: UnhealthyServiceException => throw unhealthyService
        case ex: Throwable => {
          currentState.processCallResult(wasCallSuccessful = false); throw ex
        }
      }
  }
}

class UnhealthyServiceException(message: String) extends RuntimeException(message)
