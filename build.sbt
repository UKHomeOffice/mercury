name := "mercury"

organization := "uk.gov.homeoffice"

scalaVersion := "2.11.8"

fork in run := true

fork in Test := true

publishArtifact in Test := true

initialCommands in (Test, console) := "ammonite.Main().run()"

mainClass := Some("uk.gov.homeoffice.mercury.boot.Boot")

assemblyJarName in assembly := "mercury.jar"

lazy val IT = config("it") extend Test

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .configs(IT)
  .settings(inConfig(IT)(Defaults.testSettings) : _*)
  .settings(Revolver.settings)