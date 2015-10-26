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

import org.scalatest.{Matchers, WordSpecLike}

class CircuitBreakerRepositorySpec extends WordSpecLike with Matchers {

  "CircuitBreakerRepository" should {
    "Add a circuit breaker instance when one does not already exist" in {
      val numberOfCallsToTrigger = 5
      val unhealthyDuration = 100
      val turbulencePeriod = 100

      Repository._circuitBreakers.empty
      Repository.addCircuitBreaker("cb_1", Some(numberOfCallsToTrigger), Some(unhealthyDuration),Some(turbulencePeriod))
      val actual = Repository.circuitBreaker("cb_1")
      actual shouldBe new CircuitBreaker("cb_1", numberOfCallsToTrigger, unhealthyDuration, turbulencePeriod)
    }

    "Add a circuit breaker using default values" in {
      Repository._circuitBreakers.empty
      Repository.addCircuitBreaker("cb_2", None, None, None)
      val actual = Repository.circuitBreaker("cb_2")

      val defaultDuration: Long = 5 * 60 * 1000
      val defaultNumberOfCallsToTrigger: Int = 4

      actual shouldBe new CircuitBreaker("cb_2", defaultNumberOfCallsToTrigger, defaultDuration, defaultDuration)
    }

    "Throw an exception if a 2nd circuit breaker with the same name is added" in {
      Repository._circuitBreakers.empty
      Repository.addCircuitBreaker("cb_3", None, None, None)
      intercept[IllegalArgumentException](Repository.addCircuitBreaker("cb_3", None, None, None))
    }
  }
}
