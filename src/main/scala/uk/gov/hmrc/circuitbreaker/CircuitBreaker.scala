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
import play.api.Logger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class UnhealthyServiceException(message: String) extends RuntimeException(message)

case class CircuitBreakerConfig(
    serviceName: String, 
    numberOfCallsToTriggerStateChange: Int, 
    unavailablePeriodDuration: Int,
    unstablePeriodDuration: Int) {
}

case object CircuitBreakerConfig {
  
  private val defaultDuration: Int = 5 * 60 * 1000 // 5 minutes
  private val defaultNumberOfCalls: Int = 4
  
  def apply(
    serviceName: String, 
    numberOfCallsToTriggerStateChange: Option[Int] = None, 
    unavailablePeriodDuration: Option[Int] = None,
    unstablePeriodDuration: Option[Int] = None 
  ): CircuitBreakerConfig = apply(serviceName,
    numberOfCallsToTriggerStateChange.getOrElse(defaultNumberOfCalls),
    unavailablePeriodDuration.getOrElse(defaultDuration),
    unstablePeriodDuration.getOrElse(defaultDuration)
  )
}

private[circuitbreaker] sealed abstract class StateProcessor {
	def name: String
  def processCallResult(wasCallSuccessful: Boolean): Unit
  def stateAwareInvoke[T](f: => Future[T]): Future[T] = f
}

private[circuitbreaker] sealed trait TimedState {
  def duration: Int
  private val periodStart = currentTimeMillis
  def periodElapsed: Boolean = (currentTimeMillis - periodStart > duration)
}


private[circuitbreaker] class CircuitBreaker(val config: CircuitBreakerConfig, exceptionsToBreak: Throwable => Boolean) {

  Logger.info(s"Circuit Breaker [$name] instance created with config $config")

  protected def initialState: StateProcessor = Healthy
  
  def name: String = config.serviceName
  
  def currentState: StateProcessor = synchronized { state }

  private var state: StateProcessor = initialState

  private[circuitbreaker] def setState(newState: StateProcessor) = {
    state = newState
    Logger.debug(s"Service [$name] is in state [${state.name}]")
  }
  
  private def processCallResult(wasCallSuccessful: Boolean) = synchronized {
    currentState.processCallResult(wasCallSuccessful)
  }
  
  def invoke[T](f: => Future[T]): Future[T] =
    currentState.stateAwareInvoke(f).map { x =>
      processCallResult(wasCallSuccessful = true)
      x
    } recoverWith {
      case unhealthyService: UnhealthyServiceException => 
        throw unhealthyService
      case ex: Throwable =>
        processCallResult(wasCallSuccessful = !exceptionsToBreak(ex))
        throw ex
    }
  
  protected sealed trait CountingState {
    def startCount: Int
    
    private var count = startCount
    
    def needsStateChangeAfterIncrement = {
      count += 1
      count >= config.numberOfCallsToTriggerStateChange
    }
  }
  
  protected object Healthy extends StateProcessor {
    
    def processCallResult(wasCallSuccessful: Boolean) = {
      if (!wasCallSuccessful) {
        if (config.numberOfCallsToTriggerStateChange > 1) setState(new Unstable)
        else setState(Unavailable)
      }
    }
  
    val name = "HEALTHY"
  }
  
  
  protected class Unstable extends StateProcessor with TimedState with CountingState {
    
    lazy val startCount = 1
    lazy val duration = config.unstablePeriodDuration
    
    def processCallResult(wasCallSuccessful: Boolean) = {
      if (wasCallSuccessful && periodElapsed) setState(Healthy)
      else if (!wasCallSuccessful) {
        if (periodElapsed) setState(new Unstable)
        else if (needsStateChangeAfterIncrement) setState(Unavailable)
      }
    }
  
    val name = "UNSTABLE"
  }
  
  
  protected class Trial extends StateProcessor with CountingState {
    
    lazy val startCount = 0
    
    def processCallResult(wasCallSuccessful: Boolean) = {
      if (wasCallSuccessful && needsStateChangeAfterIncrement) setState(Healthy)
      else if (!wasCallSuccessful) setState(Unavailable)
    }
  
    val name = "TRIAL"
  }
  
  
  protected object Unavailable extends StateProcessor with TimedState {
    
    lazy val duration = config.unavailablePeriodDuration
    
    def processCallResult(wasCallSuccessful: Boolean) = ()
  
    override def stateAwareInvoke[T](f: => Future[T]): Future[T] = {
      if (periodElapsed) {
        setState(new Trial)
        f
      } else {
        Future.failed(new UnhealthyServiceException(config.serviceName))
      }
    }
  
    val name = "UNAVAILABLE"
  }
}
