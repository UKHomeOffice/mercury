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
val `akka-http-version` = "10.0.2"
val `play-version` = "2.5.10"
val `scalactic-version` = "3.0.1"
val `akka-scala-lib-version` = "2.1.7"
val `aws-scala-lib-version` = "1.1.3"
val `io-scala-lib-version` = "1.9.8"
val `test-scala-lib-version` = "1.4.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % `akka-version` withSources(),
  "com.typesafe.akka" %% "akka-http" % `akka-http-version` withSources(),
  "org.scalactic" %% "scalactic" % `scalactic-version` withSources(),
  "com.softwaremill.quicklens" %% "quicklens" % "1.4.8" withSources(),
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version` withSources(),
  "com.github.UKHomeOffice" %% "aws-scala-lib" % `aws-scala-lib-version` withSources(),
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version` withSources()
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.8.6" % Test,
  "org.scalatest" %% "scalatest" % `scalactic-version` % Test withSources(),
  "com.typesafe.akka" %% "akka-testkit" % `akka-version` % Test withSources(),
  "com.typesafe.akka" %% "akka-http-testkit" % `akka-http-version` % Test withSources(),
  "com.typesafe.play" %% "play-server" % `play-version` % Test withSources(),
  "com.typesafe.play" %% "play-test" % `play-version` % Test withSources(),
  "io.findify" %% "s3mock" % "0.1.6" % Test withSources(),
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "aws-scala-lib" % `aws-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "test-scala-lib" % `test-scala-lib-version` % Test classifier "tests" withSources()
)