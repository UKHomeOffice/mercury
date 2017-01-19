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