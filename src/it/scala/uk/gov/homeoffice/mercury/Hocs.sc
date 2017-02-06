import $ivy.`org.http4s::http4s-dsl:0.15.3`
import $ivy.`org.http4s::http4s-blaze-server:0.15.3`
import $ivy.`org.http4s::http4s-blaze-client:0.15.3`

import org.http4s._, org.http4s.dsl._


val helloWorldService = HttpService {
  case GET -> Root / "hello" / name =>
    Ok(s"Hello, $name.")
}

import org.http4s.server.blaze._
import org.http4s.server.syntax._

val builder = BlazeBuilder.bindHttp(8080, "localhost").mountService(helloWorldService, "/")

val server = builder.run

val v = scala.io.StdIn.readLine()

/*
import $ivy.`com.typesafe.play::play:2.5.12`
import $ivy.`com.typesafe.play::play-netty-server:2.5.12`
import $ivy.`org.scalajy::scalaj-http:2.2.1`

import play.core.server._, play.api.routing.sird._, play.api.mvc._
import scalaj.http._

val server = NettyServer.fromRouter(new ServerConfig(
  rootDir = new java.io.File("."),
  port = Some(19000), sslPort = None,
  address = "0.0.0.0", mode = play.api.Mode.Dev,
  properties = System.getProperties,
  configuration = play.api.Configuration(
    "play.server.netty" -> Map(
      "maxInitialLineLength" -> 4096,
      "maxHeaderSize" -> 8192,
      "maxChunkSize" -> 8192,
      "log.wire" -> false,
      "eventLoopThreads" -> 0,
      "transport" -> "jdk",
      "option.child" -> Map()
    )
  )
)) {
  case GET(p"/hello/$to") => Action { Results.Ok(s"Hello $to") }
}
try {
  println(Http("http://localhost:19000/hello/bar").asString.body)
}finally{
  server.stop()
}*/
