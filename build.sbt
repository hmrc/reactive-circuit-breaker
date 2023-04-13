//import sbt.Keys.crossScalaVersions
//import sbt._

import sbt.Resolver
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val name = "reactive-circuit-breaker"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 75.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val library = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    name := "reactive-circuit-breaker",
    organization := "uk.gov.hmrc",
    scalaVersion := scala2_12,
    crossScalaVersions := List(scala2_12, scala2_13),
    majorVersion                     := 3,
  isPublicArtefact := true,
    scoverageSettings,
  scalaVersion        := "2.12.10",
  libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
  resolvers           := Seq(
    Resolver.typesafeRepo("releases")
  )
)
