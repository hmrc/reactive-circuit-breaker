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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future


class CircuitBreakerSpec extends WordSpecLike with Matchers with ScalaFutures {

  private def successfulCall: Future[Boolean] = Future.successful(true)

  private def failedCall: Future[Boolean] = throw new RuntimeException("some exception")

  val fiveMinutes: Long = 5 * 60 * 1000
  val fourCalls: Int = 4
  val serviceName = "SomeServiceName"

  "CircuitBreaker invoke" should {

    "starts up in a healthy state" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      cb.currentState.name shouldBe "HEALTHY"
    }

    "remain healthy after a successful call" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      whenReady(cb.invoke(successfulCall)) {
        result =>
          cb.currentState.name shouldBe "HEALTHY"
      }
    }

    "remain healthy after a failed call" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      cb.currentState.name shouldBe "HEALTHY"
      cb.isServiceTurbulent shouldBe true
    }

    "state change to unhealthy from healthy after a succession of failed calls that exceed threshold" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      cb.isServiceTurbulent shouldBe true
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      cb.currentState.name shouldBe "UNHEALTHY"
      cb.isServiceTurbulent shouldBe false
    }

    "remain healthy after a succession of failed calls that exceed the failed call threshold count but occur after the failed count expiry time" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, -10000)
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      intercept[Exception] {
        cb.invoke(failedCall)
      }
      cb.currentState.name shouldBe "HEALTHY"
      cb.isServiceTurbulent shouldBe false

      // The CB should be resetting all failure counts as they are not occurring during the turbulent period
      cb.registerFailedCall shouldBe 1
    }

    "state change to unhealthy from healthy after a succession of successful and failed calls that exceed threshold" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)

      intercept[Exception] {
        cb.invoke(failedCall)
      }
      cb.isServiceTurbulent shouldBe true

      intercept[Exception] {
        cb.invoke(failedCall)
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "HEALTHY"
      }

      intercept[Exception] {
        cb.invoke(failedCall)
      }

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "HEALTHY"
          cb.isServiceTurbulent shouldBe true
      }

      intercept[Exception] {
        cb.invoke(failedCall)
      }
      cb.currentState.name shouldBe "UNHEALTHY"
      cb.isServiceTurbulent shouldBe false
    }

    "remain unhealthy and return a circuit breaker exception during the time threshold period" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      cb.setUnhealthyState()
      intercept[UnhealthyServiceException] {
        cb.invoke[Boolean](successfulCall)
      }
      cb.currentState.name shouldBe "UNHEALTHY"
      cb.isServiceTurbulent shouldBe false
    }

    "state change to trial from unhealthy after a successful call executed after the time threshold period" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, -10000, fiveMinutes)
      cb.setUnhealthyState()

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "TRIAL"
          cb.isServiceTurbulent shouldBe false
      }
    }

    "state change to unhealthy from trail after a failed call" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      cb.setTrialState()
      cb.isServiceTurbulent shouldBe false
      intercept[Exception] {
        cb.invoke[Boolean](failedCall)
      }
      cb.currentState.name shouldBe "UNHEALTHY"
    }

    "remain trial state after a single successful call" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      cb.setTrialState()

      whenReady(cb.invoke[Boolean](successfulCall)) {
        result =>
          cb.currentState.name shouldBe "TRIAL"
          cb.isServiceTurbulent shouldBe false
      }
    }

    "state change from trial to healthy after the number successful calls equals the threshold amount" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      cb.setTrialState()
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

    "state change from trial to unhealthy after fewer than threshold series of successful calls and a single failed call" in {
      val cb = new CircuitBreaker(serviceName, fourCalls, fiveMinutes, fiveMinutes)
      cb.setTrialState()

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

      intercept[Exception] {
        cb.invoke[Boolean](failedCall)
      }
      cb.currentState.name shouldBe "UNHEALTHY"

      intercept[UnhealthyServiceException] {
        cb.invoke[Boolean](successfulCall)
      }
    }
  }
}
