name := "mercury"

organization := "uk.gov.homeoffice"

scalaVersion := "2.11.8"

fork in run := true

fork in Test := true

publishArtifact in Test := true

mainClass := Some("uk.gov.homeoffice.mercury.Boot")