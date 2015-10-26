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

import scala.collection.mutable

private[circuitbreaker] object Repository {

  private[circuitbreaker] var _circuitBreakers = mutable.HashMap[String, CircuitBreaker]()

  private val defaultDuration: Long = 5 * 60 * 1000 // 5 minutes
  private val defaultNumberOfCalls: Int = 4

  def addCircuitBreaker(name: String, numberOfCallsToTriggerStateChange: Option[Int],
                        unhealthyServiceUnavailableDuration: Option[Long],
                        turbulencePeriodDuration: Option[Long]): Unit = {
    _circuitBreakers.get(name) match {
      case None => _circuitBreakers += ((name, new CircuitBreaker(name, numberOfCallsToTriggerStateChange.fold(defaultNumberOfCalls)(x => x),
        unhealthyServiceUnavailableDuration.fold(defaultDuration)(x => x),
        turbulencePeriodDuration.fold(defaultDuration)(x => x))))

      case Some(existingCircuitBreaker) => throw new java.lang.IllegalArgumentException(s"CircuitBreaker [$name] is already configured")
    }
  }

  def circuitBreaker(name: String) = _circuitBreakers(name)

}
