package io.github.morgaroth.httpserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.{CorsSettings, HttpHeaderRange}
import io.github.morgaroth.httpserver.socketio.SocketIoService

import scala.concurrent.Future

class SocketIOServer(implicit sys: ActorSystem) {
  implicit val mat = ActorMaterializer()

  private val service = new SocketIoService(WorkerActor)
  val route = service.route

  private val headers = List(Origin, `Content-Type`, Accept, `Accept-Encoding`, `Accept-Language`, Host, Referer, `User-Agent`).map(_.name)

  val corsSettings = CorsSettings.defaultSettings.copy(
    allowGenericHttpRequests = true,
    allowedOrigins = HttpOriginRange.*,
    allowedHeaders = HttpHeaderRange(headers: _*)
  )

  def bind(port: Int = 8080, interface: String = "0.0.0.0"): Future[Http.ServerBinding] = {
    println(s"Server online at http://$interface:$port...")
    Http().bindAndHandle(cors(corsSettings)(route), interface, port)
  }
}