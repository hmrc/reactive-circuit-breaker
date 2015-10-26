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

private[circuitbreaker] abstract class StateProcessor {
  def processCallResult(wasCallSuccessful: Boolean)
  def stateAwareInvoke[T](f: => Future[T]): Future[T] = f
  def name: String
}

private[circuitbreaker] class Healthy(cb: CircuitBreakerModel) extends StateProcessor {
  def processCallResult(wasCallSuccessful: Boolean) = {
    if (!wasCallSuccessful && cb.registerFailedCall >= cb.numberOfCallsToChangeState && cb.isServiceTurbulent) cb.setUnhealthyState()
  }

  override def name: String = "HEALTHY"
}


private[circuitbreaker] class Trial(cb: CircuitBreakerModel) extends StateProcessor {
  def processCallResult(wasCallSuccessful: Boolean) = {
    wasCallSuccessful match {
      case  true => if (cb.registerSuccessfulTrialCall >= cb.numberOfCallsToChangeState) cb.setHealthyState()
      case false => cb.setUnhealthyState()
    }
  }

  override def name: String = "TRIAL"
}

private[circuitbreaker] class Unhealthy(cb: CircuitBreakerModel) extends StateProcessor {
  def processCallResult(wasCallSuccessful: Boolean) = {
    throw new RuntimeException("SHOULD NOT HAVE EXECUTED A CALL WHEN IN THIS STATE!")
  }

  override def stateAwareInvoke[T](f: => Future[T]): Future[T] = {
    if (cb.hasWaitTimeElapsed) {
      cb.setTrialState()
      f
    } else {
      throw new UnhealthyServiceException(cb.name)
    }
  }

  override def name: String = "UNHEALTHY"
}
