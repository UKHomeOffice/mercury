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
val `mercury-akka-lib-version` = "2.1.17"
val `mercury-aws-lib-version` = "1.2.4"
val `mercury-io-lib-version` = "1.9.12"
val `mime4j-version` = "0.8.0"
val `flying-saucer-version` = "9.1.5"

val `mercury-test-lib-version` = "1.4.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % `akka-version`,
  "com.typesafe.akka" %% "akka-http" % `akka-http-version`,
  "org.scalactic" %% "scalactic" % `scalactic-version`,
  "com.softwaremill.quicklens" %% "quicklens" % "1.4.8",
  "com.github.UKHomeOffice" %% "mercury-akka-lib" % `mercury-akka-lib-version`,
  "com.github.UKHomeOffice" %% "mercury-aws-lib" % `mercury-aws-lib-version`,
  "com.github.UKHomeOffice" %% "mercury-io-lib" % `mercury-io-lib-version`,
  "org.apache.james" % "apache-mime4j" % `mime4j-version`,
  "org.xhtmlrenderer" % "flying-saucer-core" % `flying-saucer-version`,
  "org.xhtmlrenderer" % "flying-saucer-pdf" % `flying-saucer-version` exclude("org.bouncycastle", "bcprov-jdk14")
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
  "com.github.UKHomeOffice" %% "mercury-akka-lib" % `mercury-akka-lib-version` % Test classifier "tests",
  "com.github.UKHomeOffice" %% "mercury-aws-lib" % `mercury-aws-lib-version` % Test classifier "tests",
  "com.github.UKHomeOffice" %% "mercury-io-lib" % `mercury-io-lib-version` % Test classifier "tests",
  "com.github.UKHomeOffice" %% "mercury-test-lib" % `mercury-test-lib-version` % Test classifier "tests"
)
