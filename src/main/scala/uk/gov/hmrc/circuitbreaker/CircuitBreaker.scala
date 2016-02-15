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

import java.lang.System._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UnhealthyServiceException(message: String) extends RuntimeException(message)

case class CircuitBreakerConfig(serviceName: String,
                                numberOfCallsToTriggerStateChange: Int,
                                unavailablePeriodDuration: Int,
                                unstablePeriodDuration: Int,
                                callback: Option[Callback])

case object CircuitBreakerConfig {

  private val defaultDuration: Int = 5 * 60 * 1000
  // 5 minutes
  private val defaultNumberOfCalls: Int = 4

  def apply(
             serviceName: String,
             numberOfCallsToTriggerStateChange: Option[Int] = None,
             unavailablePeriodDuration: Option[Int] = None,
             unstablePeriodDuration: Option[Int] = None,
             callback: Option[Callback] = None
           ): CircuitBreakerConfig = apply(serviceName,
    numberOfCallsToTriggerStateChange.getOrElse(defaultNumberOfCalls),
    unavailablePeriodDuration.getOrElse(defaultDuration),
    unstablePeriodDuration.getOrElse(defaultDuration),
    callback
  )
}


trait State {
  def name: String
}

case object HEALTHY extends State {
  val name = "HEALTHY"
}

case object UNSTABLE extends State {
  val name = "UNSTABLE"
}

case object TRIAL extends State {
  val name = "TRIAL"
}

case object UNAVAILABLE extends State {
  val name = "UNAVAILABLE"
}


private[circuitbreaker] sealed trait StateProcessor {
  def state: State

  def processCallResult(wasCallSuccessful: Boolean): Unit

  def stateAwareInvoke[T](f: => Future[T]): Future[T] = f
}


private[circuitbreaker] sealed trait TimedState {
  def duration: Int

  private val periodStart = currentTimeMillis

  def periodElapsed: Boolean = currentTimeMillis - periodStart > duration
}


private[circuitbreaker] class CircuitBreaker(config: CircuitBreakerConfig, exceptionsToBreak: Throwable => Boolean) {

  private val state = new AtomicReference(initialState)

  protected def initialState: StateProcessor = Healthy

  def name: String = config.serviceName

  def currentState: StateProcessor = state.get

  Logger.info(s"Circuit Breaker [$name] instance created with config $config")

  private def invokeCallback(stateProcessor: StateProcessor) = config.callback match {
    case None => Future.successful(())
    case Some(callback) => Future(callback(stateProcessor.state))
  }

  def isServiceAvailable = currentState match {
    case unavailableState: Unavailable => unavailableState.periodElapsed
    case _ => true
  }

  private[circuitbreaker] def setState(oldState: StateProcessor, newState: StateProcessor): Unit =
  /* If the stateProcessor initiating a stateProcessor change is no longer the current stateProcessor
   * we ignore this call. We are sacrificing a tiny bit of accuracy in our counters
   * for getting full thread-safety with good performance.
   */
    if (state.compareAndSet(oldState, newState)) {
      invokeCallback(newState)
    }

  def invoke[T](f: => Future[T]): Future[T] = {
    currentState.stateAwareInvoke(f).map { x =>
      currentState.processCallResult(wasCallSuccessful = true)
      x
    } recoverWith {
      case unhealthyService: UnhealthyServiceException =>
        throw unhealthyService
      case ex: Throwable =>
        currentState.processCallResult(wasCallSuccessful = !exceptionsToBreak(ex))
        throw ex
    }
  }

  protected sealed trait CountingState {
    def startCount: Int

    private val count = new AtomicInteger(startCount)

    def needsStateChangeAfterIncrement = count.incrementAndGet >= config.numberOfCallsToTriggerStateChange
  }

  protected object Healthy extends StateProcessor {

    def processCallResult(wasCallSuccessful: Boolean) = {
      if (!wasCallSuccessful) {
        if (config.numberOfCallsToTriggerStateChange > 1) setState(this, new Unstable)
        else setState(this, new Unavailable)
      }
    }

    override val state = HEALTHY
  }


  protected class Unstable extends StateProcessor with TimedState with CountingState {

    lazy val startCount = 1
    lazy val duration = config.unstablePeriodDuration

    def processCallResult(wasCallSuccessful: Boolean) = {
      if (wasCallSuccessful && periodElapsed) setState(this, Healthy)
      else if (!wasCallSuccessful) {
        if (periodElapsed) setState(this, new Unstable) // resets count
        else if (needsStateChangeAfterIncrement) setState(this, new Unavailable)
      }
    }

    override val state = UNSTABLE
  }


  protected class Trial extends StateProcessor with CountingState {

    lazy val startCount = 0

    def processCallResult(wasCallSuccessful: Boolean) = {
      if (wasCallSuccessful && needsStateChangeAfterIncrement) setState(this, Healthy)
      else if (!wasCallSuccessful) setState(this, new Unavailable)
    }

    override val state = TRIAL
  }


  protected class Unavailable extends StateProcessor with TimedState {

    lazy val duration = config.unavailablePeriodDuration

    def processCallResult(wasCallSuccessful: Boolean) = ()

    override def stateAwareInvoke[T](f: => Future[T]): Future[T] = {
      if (periodElapsed) {
        setState(this, new Trial)
        f
      } else {
        Future.failed(new UnhealthyServiceException(config.serviceName))
      }
    }

    override val state = UNAVAILABLE
  }

}

