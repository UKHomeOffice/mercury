name := "mercury"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

fork in run := true

fork in Test := true

resolvers ++= Seq(
  "Artifactory Snapshot Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
  "Artifactory Release Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-release-local/",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Kamon Repository" at "http://repo.kamon.io",
  Resolver.bintrayRepo("findify", "maven")
)

val `akka-version` = "2.4.16"
val `akka-http-version` = "10.0.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % `akka-version` withSources(),
  "com.typesafe.akka" %% "akka-http" % `akka-http-version` withSources()
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.8.6" % Test,
  "com.typesafe.akka" %% "akka-testkit" % `akka-version` % Test withSources(),
  "com.typesafe.akka" %% "akka-http-testkit" % `akka-http-version` % Test withSources()
)