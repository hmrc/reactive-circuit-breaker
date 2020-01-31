import sbt._

object AppDependencies {

  val httpCore = "uk.gov.hmrc" %% "http-verbs" % "10.5.0-play-26"
  val logbackCore = "ch.qos.logback" % "logback-core" % "1.2.3"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8" % "test"

  val compile = Seq(httpCore, logbackCore, logbackClassic)
  val test = Seq(scalaTest)
}