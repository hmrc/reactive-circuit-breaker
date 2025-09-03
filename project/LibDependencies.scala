import sbt._

object LibDependencies {

  val compile = Seq(
    "uk.gov.hmrc"    %% "http-verbs-play-30" % "15.5.0",
    "ch.qos.logback" %  "logback-classic"    % "1.5.18"
  )

  val test = Seq(
    "org.scalatest"        %% "scalatest"    % "3.2.19",
    "com.vladsch.flexmark" %  "flexmark-all" % "0.64.8"
  ).map(_ % Test)
}