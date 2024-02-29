import sbt._

object LibDependencies {

  val compile = Seq(
    "ch.qos.logback" % "logback-classic" % "1.4.11",
    "uk.gov.hmrc"     %% "http-verbs-play-30" % "14.12.0"
  )

  val test = Seq(
    "org.scalatest"   %% "scalatest"          % "3.0.8" % Test,
    "org.pegdown"     % "pegdown"             % "1.6.0" % Test
  )
}