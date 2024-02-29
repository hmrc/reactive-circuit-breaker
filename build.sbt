import sbt.Resolver
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import SCoverageSettings._

val scala2_13 = "2.13.8"

ThisBuild / majorVersion     := 5
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := scala2_13

lazy val library = (project in file("."))
  .settings(
    publish / skip := true,
    scoverageSettings,
    libraryDependencies := LibDependencies.compile ++ LibDependencies.test
  )

