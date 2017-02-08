enablePlugins(DockerPlugin, DockerComposePlugin)

dockerImageCreationTask := docker.value

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("java")
    expose(9100)
    copy(artifact, artifactTargetPath)
    cmd("java", "-jar", artifactTargetPath)
  }
}