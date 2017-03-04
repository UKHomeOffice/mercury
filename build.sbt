name := "mercury"

organization := "uk.gov.homeoffice"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:reflectiveCalls",
  "-language:postfixOps",
  "-Yrangepos",
  "-Yrepl-sync"
)

javaOptions in Test ++= Seq(
  "-Dconfig.resource=application.test.conf"
)

fork in run := true

fork in Test := true

fork in IT := true

initialCommands in (Test, console) := "ammonite.Main().run()"

mainClass := Some("uk.gov.homeoffice.mercury.boot.Boot")

assemblyJarName in assembly := "mercury.jar"

publishArtifact in Test := true

releaseIgnoreUntrackedFiles := true

enablePlugins(SiteScaladocPlugin)

lazy val IT = config("it") extend Test

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .configs(IT)
  .settings(inConfig(IT)(Defaults.testSettings) : _*)
  .settings(Revolver.settings)