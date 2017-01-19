name := "mercury"

organization := "uk.gov.homeoffice"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

fork in run := true

fork in Test := true

publishArtifact in Test := true

mainClass := Some("uk.gov.homeoffice.mercury.Boot")

enablePlugins(DockerPlugin)

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Kamon Repository" at "http://repo.kamon.io",
  "jitpack" at "https://jitpack.io",
  Resolver.bintrayRepo("hseeberger", "maven"),
  Resolver.bintrayRepo("findify", "maven")
)

val `akka-version` = "2.4.16"
val `akka-http-version` = "10.0.1"
val `scalactic-version` = "3.0.1"
val `akka-scala-lib-version` = "2.1.2"
val `aws-scala-lib-version` = "1.0.2"
val `io-scala-lib-version` = "1.9.4"
val `test-scala-lib-version` = "1.4.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % `akka-version` withSources(),
  "com.typesafe.akka" %% "akka-http" % `akka-http-version` withSources(),
  "org.scalactic" %% "scalactic" % `scalactic-version` withSources(),
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version` withSources(),
  "com.github.UKHomeOffice" %% "aws-scala-lib" % `aws-scala-lib-version` withSources(),
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version` withSources()
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.8.6" % Test,
  "org.scalatest" %% "scalatest" % `scalactic-version` % Test withSources(),
  "com.typesafe.akka" %% "akka-testkit" % `akka-version` % Test withSources(),
  "com.typesafe.akka" %% "akka-http-testkit" % `akka-http-version` % Test withSources(),
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "aws-scala-lib" % `aws-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "test-scala-lib" % `test-scala-lib-version` % Test classifier "tests" withSources()
)

assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.first
  case "application.conf" => MergeStrategy.first
  case "application.test.conf" => MergeStrategy.discard
  case "version.conf" => MergeStrategy.concat
  case PathList("org", "apache", "commons", _*) => MergeStrategy.first
  case PathList("org", "mockito", _*) => MergeStrategy.first
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("java")
    expose(8080)
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}