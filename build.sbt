import sbt.Keys.crossScalaVersions
import sbt._

val name = "reactive-circuit-breaker"

lazy val library = Project(name, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion                     := 3,
    makePublicallyAvailableOnBintray := true
  ).settings(
  scalaVersion        := "2.12.10",
  crossScalaVersions  := Seq("2.11.11"),
  libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
  resolvers           := Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.typesafeRepo("releases")
  )
)
