package io.github.morgaroth.telegrambot.core.api.methods

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.Multipart.FormData
import akka.stream.{ActorMaterializer, Materializer}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import io.github.morgaroth.base.MMarshalling
import io.github.morgaroth.telegrambot.core.api.models.formats.DI
import io.github.morgaroth.telegrambot.core.api.models.{File, ForwardMessage, GetFile, GetUserProfilePhotos, Message, SendChatAction, SendDocument, SendLocation, SendMessage, SendPhoto, Update, User, UserProfilePhotos}

trait Methods extends Json4sSupport with MMarshalling {
  implicit def actorSystem: ActorSystem

  implicit lazy val mat: Materializer = ActorMaterializer()

  def botToken: String

  def m1[T: ToEntityMarshaller, R <: Any : Manifest](name: String) = new Method1[T, R](name, botToken = botToken)

  def m0[R <: AnyRef : Manifest](name: String) = new Method0[R](name, botToken = botToken)

  lazy val getMe = m0[User]("getMe")

  lazy val getUpdates: Method1[GetUpdatesReq, List[Update]] = m1[GetUpdatesReq, List[Update]]("getUpdates")

  def m1m[R <: AnyRef : Manifest](name: String): Method1[R, Message] = m1[R, Message](name)

  lazy val sendPhoto = m1m[FormData]("sendPhoto").compose[SendPhoto](_.toForm)
  //  lazy val sendAudio = m1m[FormData]("sendAudio").compose[SendAudio](_.toForm)
  lazy val sendDocument = m1m[FormData]("sendDocument").compose[SendDocument](_.toForm)
  //  lazy val sendSticker = m1m[FormData]("sendSticker").compose[SendSticker](_.toForm)
  //  lazy val sendVideo = m1m[FormData]("sendVideo").compose[SendVideo](_.toForm)
  //  lazy val sendVoice = m1m[FormData]("sendVoice").compose[SendVoice](_.toForm)

  lazy val sendLocation = m1m[SendLocation]("sendLocation")
  lazy val sendMessage = m1m[SendMessage]("sendMessage")
  lazy val forwardMessage: Method1[ForwardMessage, Message] = m1m[ForwardMessage]("forwardMessage")
  lazy val sendChatAction = m1[SendChatAction, Boolean]("sendChatAction")
  lazy val getUserProfilePhotos = m1[GetUserProfilePhotos, UserProfilePhotos]("getUserProfilePhotos")
  lazy val getFile = m1[GetFile, File]("getFile")

  //  lazy val fetchFile: FileFetch = new FileFetch(botToken)
}

object Methods {
  def apply(botsToken: String, as: ActorSystem, materializer: Materializer): Methods = new Methods {
    override implicit def actorSystem: ActorSystem = as

    override def botToken: String = botsToken
  }

  def apply(botToken: String)(implicit as: ActorSystem, mat: Materializer, di: DI): Methods = apply(botToken, as, mat)
}