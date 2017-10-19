package io.github.morgaroth.telegrambot.core.engine

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.HttpResponse
import io.github.morgaroth.telegrambot.core.api.methods.Response
import io.github.morgaroth.telegrambot.core.api.models.Update

case class NewUpdate(id: UUID, botId: String, update: Update)

case class WebHookSettings(domain: String, port: Int, certificate: Option[java.io.File])

case class CacheUpdate(u: NewUpdate)

case class UpdateHandled(id: UUID)

case class GetRemaining(botId: String)

case class Remaining(remaining: List[NewUpdate])

case class GetRemainingFail(botID: String, exception: Throwable)

case class OK(id: UUID)

case class Fail(id: UUID, exception: Throwable)

// update registering
case class Register(botId: String, botToken: String, botActor: ActorRef)

case object Registered

case class RegisteringFailed(reason: Either[Response[Boolean], Throwable])

case class UnRegister(botId: String, botToken: String)

case object Unregistered

case class UnregisteringFailed(reason: Either[Response[Boolean], Throwable])

case class WebhookConflict(resp: HttpResponse) extends Exception
