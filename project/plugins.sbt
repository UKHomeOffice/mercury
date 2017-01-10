resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.4")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")