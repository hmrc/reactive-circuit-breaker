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

import java.lang.System._
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}
import play.api.Logger
import scala.concurrent.Future

trait UsingCircuitBreaker {

  val circuitBreakerName: String
  val numberOfCallsToTriggerStateChange: Option[Int]
  val unhealthyServiceUnavailableDuration: Option[Long]
  val turbulencePeriodDuration: Option[Long]

  Repository.addCircuitBreaker(circuitBreakerName, numberOfCallsToTriggerStateChange, unhealthyServiceUnavailableDuration, turbulencePeriodDuration)

  def withCircuitBreaker[T](f: => Future[T]): Future[T] = {
    Repository.circuitBreaker(circuitBreakerName).invoke(f)
  }

  def canServiceBeInvoked = {
    !Repository.circuitBreaker(circuitBreakerName).currentState.isCircuitBreakerTripped
  }
}

private[circuitbreaker] trait CircuitBreakerModel {
  def setUnhealthyState()
  def setTrialState()
  def setHealthyState()

  def registerFailedCall: Int
  def registerSuccessfulTrialCall: Int

  def currentState: StateProcessor
  def isServiceTurbulent: Boolean

  def hasWaitTimeElapsed: Boolean

  def numberOfCallsToChangeState: Int
  def name: String
}

private[circuitbreaker] case class CircuitBreaker(serviceName: String, numberOfCallsToTriggerStateChange: Int, unhealthyServiceUnavailableDuration: Long,
                                                  turbulencePeriodDuration: Long) extends CircuitBreakerModel with CircuitBreakerExecutor {

  Logger.info(s"Circuit Breaker [$name] instance created with numberOfCallsToTriggerStateChange [$numberOfCallsToTriggerStateChange] unhealthyServiceUnavailableDuration [$unhealthyServiceUnavailableDuration] turbulencePeriodDuration [$turbulencePeriodDuration]")

  def numberOfCallsToChangeState: Int = numberOfCallsToTriggerStateChange

  private val _trippedAt = new AtomicLong
  private val _turbulenceStartedAt = new AtomicLong

  private val _state = new AtomicReference[StateProcessor]

  private val _failedCallCount = new AtomicInteger(0)
  private val _successfulTrialCall = new AtomicInteger(0)

  setHealthyState()

  def name: String = serviceName

  def registerFailedCall: Int = {
    // Record the start of a turbulent period or the expiry of an existing one
    if (_failedCallCount.get == 0) {
      declareStartOfTurbulencePeriod()
    } else if (currentTimeMillis - _turbulenceStartedAt.get >= turbulencePeriodDuration) {
      declareStartOfTurbulencePeriod()
      resetFailedCallCount()
    }

    _failedCallCount.incrementAndGet
  }

  private def resetFailedCallCount() = _failedCallCount.set(0)

  private def resetTurbulenceStartedAt() = _turbulenceStartedAt.set(0)

  private def declareStartOfTurbulencePeriod() = _turbulenceStartedAt.set(currentTimeMillis)

  def registerSuccessfulTrialCall: Int = _successfulTrialCall.incrementAndGet

  private def resetSuccessfulTrialCall() = _successfulTrialCall.set(0)

  def currentState: StateProcessor = _state.get

  def hasWaitTimeElapsed: Boolean = {
    val elapsed = currentTimeMillis - _trippedAt.get
    if (elapsed <= unhealthyServiceUnavailableDuration) false else true
  }

  def setUnhealthyState() = {
    _state.set(new Unhealthy(this))
    _trippedAt.set(currentTimeMillis)
    resetTurbulenceStartedAt()
    Logger.debug(s"Service [$serviceName] is in state [${_state.get().name}]")
  }

  def setTrialState() = {
    _state.set(new Trial(this))
    resetSuccessfulTrialCall()
    Logger.debug(s"Service [$serviceName] is in state [${_state.get().name}]")
  }

  def setHealthyState() = {
    _state.set(new Healthy(this))
    resetFailedCallCount()
    resetSuccessfulTrialCall()
    Logger.debug(s"Service [$serviceName] is in state [${_state.get().name}]")
  }

  def isServiceTurbulent: Boolean = {
    if (currentTimeMillis - _turbulenceStartedAt.get <= turbulencePeriodDuration) true else false
  }
}