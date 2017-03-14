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

transitiveClassifiers := Seq("sources")

val `akka-version` = "2.4.17"
val `akka-http-version` = "10.0.3"
val `play-version` = "2.5.12"
val `scalactic-version` = "3.0.1"
val `ammonite-version` = "0.8.2"
val `akka-scala-lib-version` = "2.1.17"
val `aws-scala-lib-version` = "1.2.0"
val `io-scala-lib-version` = "1.9.12"
val `test-scala-lib-version` = "1.4.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % `akka-version`,
  "com.typesafe.akka" %% "akka-http" % `akka-http-version`,
  "org.scalactic" %% "scalactic" % `scalactic-version`,
  "com.softwaremill.quicklens" %% "quicklens" % "1.4.8",
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version`,
  "com.github.UKHomeOffice" %% "aws-scala-lib" % `aws-scala-lib-version`,
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version`
)

libraryDependencies ++= Seq(
  "com.lihaoyi" % "ammonite" % `ammonite-version` % Test cross CrossVersion.full,
  "org.specs2" %% "specs2-core" % "3.8.6" % Test,
  "org.scalatest" %% "scalatest" % `scalactic-version` % Test,
  "com.typesafe.akka" %% "akka-testkit" % `akka-version` % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % `akka-http-version` % Test,
  "com.typesafe.play" %% "play-server" % `play-version` % Test,
  "com.typesafe.play" %% "play-test" % `play-version` % Test,
  "io.findify" %% "s3mock" % "0.1.6" % Test,
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version` % Test classifier "tests",
  "com.github.UKHomeOffice" %% "aws-scala-lib" % `aws-scala-lib-version` % Test classifier "tests",
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version` % Test classifier "tests",
  "com.github.UKHomeOffice" %% "test-scala-lib" % `test-scala-lib-version` % Test classifier "tests"
)