/*
 * Copyright 2016 HM Revenue & Customs
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

/** Trait to be mixed in to services or connectors that wish to
 *  protect their outgoing calls from wasting unsuccessful invocations
 *  in periods where the service seems to be unavailable.
 */
trait UsingCircuitBreaker {

  /** The configuration for the circuit breaker:
   *  
   * - `serviceName` - the name of the service 
   * - `numberOfCallsToTriggerStateChange` - the number of failed calls that
   *   need to accumulate within `unstablePeriodDuration` for the service to get
   *   disabled, as well as the number of successful calls that are needed to 
   *   get a service back from the disabled state to normal. 
   * - `unavailablePeriodDuration` - the time in seconds that a service should
   *   be disabled in case it accumulated more than the configured maximum number
   *   of failures
   * - `unstablePeriodDuration` - the time in seconds that failure counts are
   *   accumulated. When the period ends without reaching the limit, the counter
   *   switches back to 0
   * - `callback` - an optional callback to be invoked on state transitions.
    *  The callback has type [[Callback]] and takes one parameter which is a [[State]] and returns [[Unit]].
   */
  protected def circuitBreakerConfig: CircuitBreakerConfig
  
  /** Predicate that defines the exceptions that should be treated as a failure.
   *  In most cases only 5xx status responses should be treated as a server-side
   *  issue.
   */
  protected def breakOnException(t: Throwable): Boolean

  /** Indicates whether the service is available. Returns `false` if the service
   *  is disabled due to accumulating too many failures in the configured time
   *  frame. Note that due to the asynchronous nature of the circuit breaker,
   *  you can still get an `UnhealthyServiceException` after this method returned
   *  `true` as the state might change any time.
   */
  protected def isServiceAvailable = circuitBreaker.isServiceAvailable

  /** The `CircuitBreaker` instance used by this trait.
   */
  protected lazy val circuitBreaker = new CircuitBreaker(circuitBreakerConfig, breakOnException)

  /** Protects the specified future from being evaluated in case the service
   *  is disabled due to accumulating too many failures in the configured time
   *  frame. If the service is disabled, the future will fail with a `UnhealthyServiceException`,
   *  if it is enabled, it will succeed or fail with whatever result the original future produces.
   */
  protected def withCircuitBreaker[T](f: => Future[T]): Future[T] = circuitBreaker.invoke(f)

  /**
    * Returns a [[State]] that exposes the current state of the circuit breaker.
    *
    * @return [[State]]
    */
  protected def currentState: State = circuitBreaker.currentState.state

}
