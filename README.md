
reactive-circuit-breaker
====
[![Build Status](https://travis-ci.org/hmrc/reactive-circuit-breaker.svg?branch=master)](https://travis-ci.org/hmrc/reactive-circuit-breaker) [ ![Download](https://api.bintray.com/packages/hmrc/releases/reactive-circuit-breaker/images/download.svg) ](https://bintray.com/hmrc/releases/reactive-circuit-breaker/_latestVersion)

Reactive Circuit Breaker is a library that maintains the state of a downstream resource's health through usage of downstream services rather than using a 'health check' call.
Each 'connector' to a downstream service will need to mixin trait in addition to supplying variables that pertain upon which the circuit breaker for a each circuit breaker to behave.
Every function/method that needs circuit breaker protection then wraps the actual function call (that returns a Future) with the function that's available through the mixin of the circuit breaker trait.

A downstream service, in the context of the circuit breaker, can be in a number of finite states:
1. Healthy
2. Trial
3. Unhealthy

The transition between the states is based on the variables supplied to the circuit breaker for a downstream service.

When the circuit breaker for a downstream service is in an Unhealthy state, all requests to that service will result in a UnhealthyServiceException returned.
Note: In this occurrence, the downstream service is never called.

##Download reactive-circuit-breaker
====
```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "reactive-circuit-breaker" % "x.x.x"
```

##Future Enhancements
====
* N/A

##License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").