import sbt.Resolver
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

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

val scala2_12 = "2.12.12"
val scala2_13 = "2.13.8"

lazy val root =(project in file("."))
  .settings(
    name := "reactive-circuit-breaker",
    majorVersion := 3,
    scalaVersion := scala2_12,
    crossScalaVersions := List(scala2_12, scala2_13),
    isPublicArtefact := true,
    scoverageSettings,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    ),
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
  )
