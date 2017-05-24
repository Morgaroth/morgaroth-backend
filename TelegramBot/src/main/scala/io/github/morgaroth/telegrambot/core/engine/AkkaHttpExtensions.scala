package io.github.morgaroth.telegrambot.core.engine

import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCode}
import akka.stream.Materializer
import io.github.morgaroth.base.MMarshalling

import scala.concurrent.Future

/**
  * Created by mateusz on 3/16/17.
  */
sealed trait UnsuccessfulResponseException extends Exception {
  def resp: HttpResponse
}

object UnsuccessfulResponseException {
  def unapply(arg: Throwable): Option[(StatusCode, HttpResponse)] = {
    arg match {
      case r: UnsuccessfulResponseException => Some((r.resp.status, r.resp))
      case _ => None
    }
  }
}

case class HttpNotFoundException(resp: HttpResponse) extends UnsuccessfulResponseException

case class HttpUnauthorizedException(resp: HttpResponse) extends UnsuccessfulResponseException

case class HttpBadRequestException(resp: HttpResponse) extends UnsuccessfulResponseException

case class HttpBadGatewayException(resp: HttpResponse) extends UnsuccessfulResponseException

case class HttpInternalServerErrorException(resp: HttpResponse) extends UnsuccessfulResponseException

case class HttpClientErrorException(resp: HttpResponse) extends UnsuccessfulResponseException

case class HttpServerErrorException(resp: HttpResponse) extends UnsuccessfulResponseException

trait AkkaHttpExtensions extends MMarshalling {

  def handleUnsuccessfulResponse(resp: HttpResponse): HttpResponse = {
    resp.status match {
      case NotFound => throw HttpNotFoundException(resp)
      case Unauthorized => throw HttpUnauthorizedException(resp)
      case BadRequest => throw HttpBadRequestException(resp)
      case BadGateway => throw HttpBadGatewayException(resp)
      case InternalServerError => throw HttpInternalServerErrorException(resp)
      case _: ClientError => throw HttpClientErrorException(resp)
      case _: ServerError => throw HttpServerErrorException(resp)
      case _ => resp
    }
  }

  import scala.concurrent.duration._

  def request[R <: AnyRef : Manifest](req: HttpRequest, debug: Boolean = true)(implicit mat: Materializer, as: ActorSystem): Future[R] = {
    import as.dispatcher
    lazy val log = if (debug) Logging(as, "requests") else ???
    val reqIq = UUID.randomUUID()
    if (debug) log.debug(s"req $reqIq: ${req.toString.replaceAll("[\n\t]+", " ")}")
    for {
      rawRes <- Http().singleRequest(req)
      _ = if (debug) log.debug(s"response of $reqIq: $rawRes")
      res <- Future.successful(rawRes).map(handleUnsuccessfulResponse)
      ent <- res.entity.toStrict(5.seconds).map(_.getData().decodeString("utf-8"))
      _ = if (debug) log.debug(s"response body of $reqIq: $ent")
      unm = MJson.read[R](ent)
    } yield unm
  }
}