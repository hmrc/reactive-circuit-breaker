import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import SCoverageSettings._

val scala2_13 = "2.13.12"

ThisBuild / majorVersion     := 5
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := scala2_13

lazy val root = (project in file("."))
  .settings(
    name := "reactive-circuit-breaker",
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
    scoverageSettings,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    ),
  )
  .disablePlugins(JUnitXmlReportPlugin)

