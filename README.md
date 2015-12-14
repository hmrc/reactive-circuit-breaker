
reactive-circuit-breaker
====
[![Build Status](https://travis-ci.org/hmrc/reactive-circuit-breaker.svg?branch=master)](https://travis-ci.org/hmrc/reactive-circuit-breaker) [ ![Download](https://api.bintray.com/packages/hmrc/releases/reactive-circuit-breaker/images/download.svg) ](https://bintray.com/hmrc/releases/reactive-circuit-breaker/_latestVersion)

Reactive Circuit Breaker is a library that maintains the state of an upstream resource's health through usage of upstream services rather than using a 'health check' call.
Each 'connector' to an upstream service will need to mix in the `UsingCircuitBreaker` trait and configure its behaviour through implementing all abstract members of the trait.
Every function/method that needs circuit breaker protection then wraps the actual function call (that returns a Future) with the `withCircuitBreaker` method
that's available through the mixin.

An upstream service, in the context of the circuit breaker, can be in a number of finite states:
1. Healthy
2. Unstable
3. Unavailable
4. Trial

The transition between the states is based on the variables supplied to the circuit breaker for a upstream service.

When the circuit breaker for a upstream service is in the Unavailable state, all requests to that service will result in a UnhealthyServiceException returned.
Note: In this occurrence, the upstream service is never called.

For configuration details see the [scaladoc](src/main/scala/uk/gov/hmrc/circuitbreaker/UsingCircuitBreaker.scala) of this mixin. 

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