/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future


class UsingCircuitBreakerSpec extends AnyWordSpec with Matchers with Eventually with ScalaFutures {

  private def returnOk = Future.successful(true)
  implicit val hc: HeaderCarrier = HeaderCarrier()
  "WithCircuitBreaker" should {
    
    "return the function result when no exception is thrown" in new UsingCircuitBreaker {
      lazy val circuitBreakerConfig = CircuitBreakerConfig( 
        serviceName = "someServiceCircuitBreaker"
      )
      
      protected def breakOnException(t: Throwable) = true

      whenReady(withCircuitBreaker[Boolean](returnOk)) {
        actualResult =>
          actualResult shouldBe true
      }
    }

    def throwException = Future.failed(new Exception("some exception"))

    "return a circuit breaker exception when the function throws an exception" in new UsingCircuitBreaker {
      lazy val circuitBreakerConfig = CircuitBreakerConfig( 
        serviceName = "test_2"
      )
      
      protected def breakOnException(t: Throwable) = true

      withCircuitBreaker[Boolean](throwException).failed.futureValue
      withCircuitBreaker[Boolean](throwException).failed.futureValue
      withCircuitBreaker[Boolean](throwException).failed.futureValue

      circuitBreaker.currentState.name shouldBe "UNSTABLE"
      isServiceAvailable shouldBe true

      withCircuitBreaker[Boolean](throwException).failed.futureValue

      circuitBreaker.currentState.name shouldBe "UNAVAILABLE"
      isServiceAvailable shouldBe false

      withCircuitBreaker[Boolean](throwException).failed.futureValue shouldBe an[UnhealthyServiceException]

      circuitBreaker.currentState.name shouldBe "UNAVAILABLE"
    }

  }
}
