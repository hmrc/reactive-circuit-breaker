import sbt._

object LibDependencies {

  val compile = Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.6",
    "uk.gov.hmrc"     %% "http-verbs-play-30" % "15.1.0"
  )

  val test = Seq(
    "org.scalatest"   %% "scalatest"          % "3.2.19" % Test,
    "com.vladsch.flexmark"     % "flexmark-all"             % "0.64.8" % Test
  )
}