import sbt._

object LibDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "http-verbs-play-28" % "14.9.0",
    "ch.qos.logback" % "logback-core" % "1.2.3",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )

  lazy val test = Seq(
    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "org.pegdown" % "pegdown" % "1.6.0" % Test
  )
}