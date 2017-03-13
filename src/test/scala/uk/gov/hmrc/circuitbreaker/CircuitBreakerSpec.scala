/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.Logger
import play.api.LoggerLike

import scala.concurrent.Future

class CircuitBreakerSpec extends WordSpecLike with Matchers with ScalaFutures {

  private def successfulCall: Future[Boolean] = Future.successful(true)

  private def failedCall: Future[Boolean] = Future.failed(new RuntimeException("some exception"))

  private def expectedFailure: Future[Boolean] = Future.failed(new ExpectedException)

  val fiveMinutes: Int = 5 * 60 * 1000
  val fourCalls: Int = 4
  val serviceName = "SomeServiceName"

  class ExpectedException extends RuntimeException("ExpectedException")

  val defaultConfig = CircuitBreakerConfig(serviceName, fourCalls, fiveMinutes, fiveMinutes)
  val defaultExceptions: Throwable => Boolean = (_ => true)

  val filteredExceptions: Throwable => Boolean = {
    case _: ExpectedException => false
    case _ => true
  }

  "CircuitBreaker invoke" should {

    "starts up in a healthy state" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions)
      cb.currentState.name shouldBe "HEALTHY"
    }

    "remain healthy after a successful call" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions)
      whenReady(cb.invoke(successfulCall)) {
        result =>
          cb.currentState.name shouldBe "HEALTHY"
      }
    }

    "become unstable after a failed call" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions)

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNSTABLE"
    }

    "state change to unhealthy from healthy after a succession of failed calls that exceed threshold" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions)
      cb.invoke(failedCall).failed.futureValue
      cb.invoke(failedCall).failed.futureValue
      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNSTABLE"

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNAVAILABLE"
    }

    "state change to unhealthy from healthy only after a succession of failed calls that are configured to break that exceed threshold" in {
      val cb = new CircuitBreaker(defaultConfig, filteredExceptions)
      cb.invoke(expectedFailure).failed.futureValue
      cb.currentState.name shouldBe "HEALTHY"

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNSTABLE"

      cb.invoke(expectedFailure).failed.futureValue
      cb.invoke(expectedFailure).failed.futureValue
      cb.invoke(expectedFailure).failed.futureValue
      cb.invoke(failedCall).failed.futureValue
      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNSTABLE"

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNAVAILABLE"
    }

    "remain unstable after a succession of failed calls that exceed the failed call threshold count but occur after the failed count expiry time" in {
      val cb = new CircuitBreaker(defaultConfig.copy(unstablePeriodDuration = -5), defaultExceptions)
      cb.invoke(failedCall).failed.futureValue
      cb.invoke(failedCall).failed.futureValue
      cb.invoke(failedCall).failed.futureValue
      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNSTABLE"
    }

    "state change to unhealthy from unstable after a succession of successful and failed calls that exceed threshold" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions)

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNSTABLE"

      cb.invoke(failedCall).failed.futureValue

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "UNSTABLE"
      }

      cb.invoke(failedCall).failed.futureValue

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "UNSTABLE"
      }

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNAVAILABLE"
    }

    "remain unhealthy and return a circuit breaker exception during the time threshold period" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions) {
        override def initialState = new Unavailable
      }

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNAVAILABLE"
    }

    "state change to trial from unhealthy after a successful call executed after the time threshold period" in {
      val cb = new CircuitBreaker(defaultConfig.copy(unavailablePeriodDuration = -5), defaultExceptions) {
        override def initialState = new Unavailable
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        _ =>
          cb.currentState.name shouldBe "TRIAL"
      }
    }

    "state change to unhealthy from trail after a failed call" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions) {
        override def initialState = new Trial
      }

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNAVAILABLE"
    }

    "remain trial state after a single successful call" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions) {
        override def initialState = new Trial
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "TRIAL"
      }
    }

    "state change from trial to healthy after the number successful calls equals the threshold amount" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions) {
        override def initialState = new Trial
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "TRIAL"
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "TRIAL"
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "HEALTHY"
      }
    }

    "state change from trial to healthy after the number of calls with expected exceptions equals the threshold amount" in {
      val cb = new CircuitBreaker(defaultConfig, filteredExceptions) {
        override def initialState = new Trial
      }

      cb.invoke(expectedFailure).failed.futureValue
      cb.invoke(expectedFailure).failed.futureValue
      cb.invoke(expectedFailure).failed.futureValue
      cb.currentState.name shouldBe "TRIAL"

      cb.invoke(expectedFailure).failed.futureValue
      cb.currentState.name shouldBe "HEALTHY"
    }

    "state change from trial to healthy after the number of calls with expected exceptions or successful calls equals the threshold amount" in {
      val cb = new CircuitBreaker(defaultConfig, filteredExceptions) {
        override def initialState = new Trial
      }

      cb.invoke(successfulCall).futureValue
      cb.invoke(expectedFailure).failed.futureValue
      cb.invoke(successfulCall).futureValue
      cb.currentState.name shouldBe "TRIAL"

      cb.invoke(expectedFailure).failed.futureValue
      cb.currentState.name shouldBe "HEALTHY"
    }

    "state change from trial to unhealthy after an unexpected exception is thrown" in {
      val cb = new CircuitBreaker(defaultConfig, filteredExceptions) {
        override def initialState = new Trial
      }

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNAVAILABLE"
    }

    "state change from trial to unhealthy after fewer than threshold series of successful calls and a single failed call" in {
      val cb = new CircuitBreaker(defaultConfig, defaultExceptions) {
        override def initialState = new Trial
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "TRIAL"
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "TRIAL"
      }

      cb.invoke(failedCall).failed.futureValue
      cb.currentState.name shouldBe "UNAVAILABLE"

      cb.invoke[Boolean](successfulCall).failed.futureValue shouldBe a[UnhealthyServiceException]
    }
  }

  "CircuitBreaker isServiceAvailable" should {

    val unavailablePeriod = 100
    val shorterThanUnavailablePeriod = 80
    val longerThanUnavailablePeriod = 120

    "return false if circuit breaker is in Unavailable state and unavailable period has not elapsed" in {
      val cb = new CircuitBreaker(CircuitBreakerConfig(serviceName, fourCalls, unavailablePeriod, fiveMinutes), defaultExceptions) {
        override def initialState = new Unavailable
      }

      Thread.sleep(shorterThanUnavailablePeriod)
      cb.isServiceAvailable shouldBe false
    }

    "return true if circuit breaker is in Unavailable state and unavailable period has elapsed" in {
      val cb = new CircuitBreaker(CircuitBreakerConfig(serviceName, fourCalls, unavailablePeriod, fiveMinutes), defaultExceptions) {
        override def initialState = new Unavailable
      }

      Thread.sleep(longerThanUnavailablePeriod)
      cb.isServiceAvailable shouldBe true
    }

    "return true if circuit breaker is in Healthy, Unstable or Trial states" in {
      val cb = new CircuitBreaker(CircuitBreakerConfig(serviceName, fourCalls, fiveMinutes, fiveMinutes), defaultExceptions) {

        val states = List(Healthy, new Unstable, new Trial)
      }

      cb.states.foreach { state =>
        cb.setState(cb.currentState, state)

        cb.isServiceAvailable shouldBe true
      }
    }
  }

  "CircuitBreaker" should {

    "log the initial state after creation" in {
      val stubbedLogger = new LoggerLikeStub()

      val cb = new CircuitBreaker(defaultConfig, defaultExceptions) {
        override def getLogger = stubbedLogger
      }

      stubbedLogger.logMessages.size shouldBe 1
      stubbedLogger.logMessages.head shouldBe s"circuitbreaker: Service [$serviceName] is in state [HEALTHY]"
    }


    "log state information whenever state changes" in {
      val stubbedLogger = new LoggerLikeStub()

      val cb = new CircuitBreaker(defaultConfig, defaultExceptions) {
        override def getLogger = stubbedLogger

        setState(currentState, new Unavailable)
      }

      stubbedLogger.logMessages.size shouldBe 2
      stubbedLogger.logMessages.last shouldBe s"circuitbreaker: Service [$serviceName] is in state [UNAVAILABLE]"
    }


    "should not log when the current state and old state are different" in {
      val stubbedLogger = new LoggerLikeStub()

      val cb = new CircuitBreaker(defaultConfig, defaultExceptions) {
        override def getLogger = stubbedLogger

        setState(new Unstable, new Unavailable)
      }

      stubbedLogger.logMessages.size shouldBe 1
    }
  }
}

class LoggerLikeStub extends LoggerLike {

  import scala.collection.mutable.Buffer

  val logMessages: Buffer[String] = Buffer()

  override val logger: Logger = null

  override def warn(msg: => String) = logMessages += msg

  override def info(msg: => String) = () // ignore

}
