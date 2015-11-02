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

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future


class WithCircuitBreakerSpec extends WordSpecLike with Matchers with Eventually with ScalaFutures {

  private def returnOk = Future.successful(true)

  "WithCircuitBreaker" should {
    "return the function result when no exception is thrown" in new UsingCircuitBreaker {
      lazy override val circuitBreakerName = "someServiceCircuitBreaker"
      lazy override val numberOfCallsToTriggerStateChange: Option[Int] = None
      lazy override val unhealthyServiceUnavailableDuration: Option[Long] = None
      lazy override val turbulencePeriodDuration: Option[Long] = None

      whenReady(withCircuitBreaker[Boolean](returnOk)) {
        actualResult =>
          actualResult shouldBe true
      }
    }

    def throwException = throw new Exception("some exception")

    "return a circuit breaker exception when the function throws an exception" in new UsingCircuitBreaker {
      lazy override val circuitBreakerName = "test_2"
      lazy override val numberOfCallsToTriggerStateChange: Option[Int] = None
      lazy override val unhealthyServiceUnavailableDuration: Option[Long] = None
      lazy override val turbulencePeriodDuration: Option[Long] = None

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      Repository.circuitBreaker(circuitBreakerName).currentState.name shouldBe "HEALTHY"

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      Repository.circuitBreaker(circuitBreakerName).currentState.name shouldBe "UNHEALTHY"

      intercept[UnhealthyServiceException] {
        withCircuitBreaker[Boolean](throwException)
      }.getMessage shouldBe "test_2"

      Repository.circuitBreaker(circuitBreakerName).currentState.name shouldBe "UNHEALTHY"
    }

    "canServiceBeInvoked" should {

      "return result according to the current state of the circuit breaker" in new UsingCircuitBreaker {
        lazy override val circuitBreakerName = "canServiceBeInvoked"
        lazy override val numberOfCallsToTriggerStateChange: Option[Int] = Some(2)
        lazy override val unhealthyServiceUnavailableDuration: Option[Long] = Some(10)
        lazy override val turbulencePeriodDuration: Option[Long] = None

        canServiceBeInvoked shouldBe true

        intercept[Exception] {
          withCircuitBreaker[Boolean](throwException)
        }

        canServiceBeInvoked shouldBe true

        intercept[Exception] {
          withCircuitBreaker[Boolean](throwException)
        }

        canServiceBeInvoked shouldBe false

        Thread.sleep(unhealthyServiceUnavailableDuration.get + 1)

        canServiceBeInvoked shouldBe true
      }
    }
  }
}
