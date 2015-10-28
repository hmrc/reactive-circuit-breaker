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
import org.specs2.mock.MockitoMocker
import org.specs2.mock.mockito.CalledMatchers

import scala.concurrent.Future

class CircuitBreakerStatesSpec extends WordSpecLike with Matchers with MockitoMocker with CalledMatchers {

"Healthy State" should {
    "remain healthy when the count of failed calls has not exceeded the threshold" in {
      val mockCBM = mock[CircuitBreakerModel]
      when(mockCBM.registerFailedCall).thenReturn(4)
      when(mockCBM.numberOfCallsToChangeState).thenReturn(5)

      val underTest = new Healthy(mockCBM)
      underTest.processCallResult(false)

      no(mockCBM).setUnhealthyState()
      no(mockCBM).setTrialState()
    }

    "change to unhealthy when the count of failed calls exceeds the failed count threshold and the service is turbulent" in {
      val mockCBM = mock[CircuitBreakerModel]
      when(mockCBM.registerFailedCall).thenReturn(4)
      when(mockCBM.numberOfCallsToChangeState).thenReturn(3)
      when(mockCBM.isServiceTurbulent).thenReturn(true)

      val underTest = new Healthy(mockCBM)
      underTest.processCallResult(false)

      one(mockCBM).isServiceTurbulent
      one(mockCBM).setUnhealthyState()
      no(mockCBM).setTrialState()
    }

    "remain healthy when a succession of calls fail that exceed the tolerable count and the service is not turbulent" in {
      val mockCBM = mock[CircuitBreakerModel]
      when(mockCBM.registerFailedCall).thenReturn(4)
      when(mockCBM.numberOfCallsToChangeState).thenReturn(3)
      when(mockCBM.isServiceTurbulent).thenReturn(false)

      val underTest = new Healthy(mockCBM)
      underTest.processCallResult(false)

      no(mockCBM).setUnhealthyState()
      no(mockCBM).setTrialState()
    }
  }

  "Trial State" should {
    "change to an unhealthy state when a single call fails" in {
      val mockCBM = mock[CircuitBreakerModel]

      val underTest = new Trial(mockCBM)
      underTest.processCallResult(false)

      one(mockCBM).setUnhealthyState()
      no(mockCBM).setTrialState()
      no(mockCBM).setHealthyState()
    }

    "remain in a trial state when the number of healthy calls has not exceeded the trail threshold needed to change state" in {
      val mockCBM = mock[CircuitBreakerModel]
      when(mockCBM.registerSuccessfulTrialCall).thenReturn(1)
      when(mockCBM.numberOfCallsToChangeState).thenReturn(3)

      val underTest = new Trial(mockCBM)
      underTest.processCallResult(true)

      no(mockCBM).setUnhealthyState()
      no(mockCBM).setTrialState()
      no(mockCBM).setHealthyState()
    }

    "change to a healthy state when the number of healthy calls has is equal to the trail threshold needed to change state" in {
      val mockCBM = mock[CircuitBreakerModel]
      when(mockCBM.registerSuccessfulTrialCall).thenReturn(3)
      when(mockCBM.numberOfCallsToChangeState).thenReturn(3)

      val underTest = new Trial(mockCBM)
      underTest.processCallResult(true)

      one(mockCBM).setHealthyState()
      no(mockCBM).setUnhealthyState()
      no(mockCBM).setTrialState()
    }
  }

  "Unhealthy State" should {
    "throw an exception if a call has been made" in {
      val mockCBM = mock[CircuitBreakerModel]

      val underTest = new Unhealthy(mockCBM)

      intercept[Exception] {
        underTest.processCallResult(true)
      }.getMessage shouldBe "SHOULD NOT HAVE EXECUTED A CALL WHEN IN THIS STATE!"

      no(mockCBM).setUnhealthyState()
      no(mockCBM).setTrialState()
      no(mockCBM).setHealthyState()
    }

    "throw an exception during the invoke if the cool-down period has not elapsed" in {
      val mockCBM = mock[CircuitBreakerModel]
      when(mockCBM.hasWaitTimeElapsed).thenReturn(false)

      val underTest = new Unhealthy(mockCBM)

      intercept[Exception] {
        underTest.stateAwareInvoke(Future.successful("test"))
      }
    }

    "change to trial state during the invoke if the cool-down period has elapsed" in {
      val mockCBM = mock[CircuitBreakerModel]
      when(mockCBM.hasWaitTimeElapsed).thenReturn(true)

      val underTest = new Unhealthy(mockCBM)

      underTest.stateAwareInvoke(Future.successful("test"))

      one(mockCBM).setTrialState()
      no(mockCBM).setUnhealthyState()
      no(mockCBM).setHealthyState()
    }
  }
}
