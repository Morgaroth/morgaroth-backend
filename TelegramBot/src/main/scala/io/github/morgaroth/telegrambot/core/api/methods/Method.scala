package io.github.morgaroth.telegrambot.core.api.methods

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.stream.Materializer
import akka.util.Timeout
import io.github.morgaroth.telegrambot.core.engine.AkkaHttpExtensions

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

case class Response[T](ok: Boolean, result: Either[String, T], description: Option[String])

trait MethodsCommons {
  implicit def as: ActorSystem

  implicit val timeout: Timeout = 30.seconds
  lazy val log = Logging(as, getClass)

  implicit def ex = as.dispatcher

  def botToken: String

  val service = "https://api.telegram.org"

  def uri(method: String): String = {
    s"$service/bot$botToken/$method"
  }
}

class Method1[T: ToEntityMarshaller, R <: Any : Manifest](endpoint: String, val botToken: String)(implicit val as: ActorSystem, mat: Materializer) extends ((T) => Future[Response[R]])
  with MethodsCommons with AkkaHttpExtensions {

  import akka.http.scaladsl.client.RequestBuilding._

  override def apply(data: T): Future[Response[R]] = {
    request[Response[R]](Post(uri(endpoint), data), debug = false)
  }
}

class Method0[R <: AnyRef : Manifest](endpoint: String, val botToken: String)(implicit val as: ActorSystem, mat: Materializer) extends (() => Future[Response[R]]) with MethodsCommons with AkkaHttpExtensions {
  this: MethodsCommons =>

  override def apply(): Future[Response[R]] = {
    request[Response[R]](Post(uri(endpoint)), debug = false)
  }
}

//class FileFetch(val botToken: String)(implicit val as: ActorSystem) extends ((String) => Future[Array[Byte]]) with MethodsCommons {
//  this: MethodsCommons =>
//  override def apply(filePath: String): Future[Array[Byte]] = {
//    val pipe = loggedSendReceive
//    pipe(Get(s"$service/file/bot$botToken/$filePath")).map(_.entity.data.toByteArray)
//  }
//}