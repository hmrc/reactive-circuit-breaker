import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import SCoverageSettings._

val scala2_13 = "2.13.12"

ThisBuild / majorVersion     := 5
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := scala2_13

lazy val root = (project in file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    publish / skip := true,
    name := "reactive-circuit-breaker",
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
    scoverageSettings
  )

