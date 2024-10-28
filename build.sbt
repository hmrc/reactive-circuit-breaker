import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import SCoverageSettings._


ThisBuild / majorVersion     := 6
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := "3.5.1"

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

