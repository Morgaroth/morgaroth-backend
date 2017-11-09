package io.github.morgaroth.telegrambot.core.engine.core

import java.util.UUID

import akka.actor._
import akka.stream.ActorMaterializer
import io.github.morgaroth.telegrambot.core.api.methods.{Methods, Response}
import io.github.morgaroth.telegrambot.core.api.models.{Chat, Command, DeleteMessage, File, ForwardMessage, GetFile, GetUserProfilePhotos, SendChatAction, SendDocument, SendLocation, SendMessage, SendPhoto, User}
import io.github.morgaroth.telegrambot.core.engine._
import io.github.morgaroth.telegrambot.core.engine.core.BotActor._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.{implicitConversions, reflectiveCalls}
import scala.util.{Failure, Success, Try}

object BotActor {
  def props(botName: String, botToken: String, updatesActor: ActorRef, worker: ActorRef): Props =
    Props(new BotActor(botName, botToken, updatesActor, worker))

  trait State

  case class Handled(id: UUID)

  case class HandledUpdate(uid: UUID, response: Command)

  case class SendMapped(response: Command, fun: PartialFunction[Any, Unit])

  case class FetchFile(f: File, onComplete: Try[Array[Byte]] => Unit)

  case class FileFetchingResult(file: File, author: Chat, `type`: String, result: Try[Array[Byte]])

  object HandledUpdate {
    def apply(u: NewUpdate, response: Command): HandledUpdate = apply(u.id, response)
  }

  case class InitializationFailed(reason: Either[Response[Boolean], Throwable]) extends State

  case object GetState

  case object Initialized extends State

}

class BotActor(botName: String, val botToken: String, updatesActor: ActorRef, worker: ActorRef)
  extends Actor with ActorLogging with Methods {

  import context.dispatcher

  var me: User = _
  getMe().onComplete {
    case Success(r) =>
      me = r.result.right.get
      worker ! me
    case Failure(t) =>
      log.warning("get me end with {}", t)
  }

  updatesActor ! Register(botName, botToken, self)

  implicit def wrapIntoLoggable[T](f: Future[Response[T]]): Object {def logoutResult: Future[Response[T]]} = new {
    def logoutResult = {
      f.onComplete {
        case Success(_) =>
        // log.info(s"request end with $result")
        case Failure(t: UnsuccessfulResponseException) =>
          t.resp.entity.toStrict(5.seconds).map(_.data.decodeString("utf-8")).onComplete {
            case Success(someData) => log.error(s"invalid response ent $someData response ${t.resp}")
            case Failure(thr) => log.error(thr, "invalid response")
          }
        case Failure(t) =>
          log.error(t, "error during executing request")
        case wtf =>
          log.error(s"WTF unhandled in logout result method $wtf")
      }
      f
    }

  }

  override def receive: Receive = initializing

  def initializing: Receive = {
    case Registered =>
      log.info("registered successfully for updates")
      context become working
      worker ! Initialized

    case RegisteringFailed(reason) =>
      worker ! InitializationFailed(reason)
      // todo stop updates actor?
      // todo stop cache actor?
      context stop self
  }

  def working: Receive = {
    case u: NewUpdate =>
      log.debug(s"forwarding update ${u.id}")
      worker ! u

    case Handled(id) =>
      log.debug(s"update $id marked as handled")

    case h: UpdateHandled =>
      log.debug(s"update ${h.id} marked as handled")

    case HandledUpdate(uId, response) if handleCommands(sender()).isDefinedAt(response) =>
      log.info(s"handling return from worker $response")
      self ! Handled(uId)
      handleCommands(sender())(response)

    case someCommand: Command if handleCommands(sender()).isDefinedAt(someCommand) =>
      log.info(s"handling command $someCommand")
      handleCommands(sender())(someCommand)

    case SendMapped(comm, onSucc) =>
      handleCommands(sender(), onSucc)(comm)

    case OK(id) =>

    //    case FetchFile(fpath, callback) =>
    //      fetchFile(fpath.file_path.get).onComplete {
    //        r => callback(r)
    //      }

    case unhandled =>
      log.warning(s"unhandled message $unhandled")
  }

  def handleCommands(requester: ActorRef, callback: PartialFunction[Any, Unit] = {
    case _ =>
  }): PartialFunction[Command, Unit] = {
    case c: SendPhoto => sendPhoto(c).map { x => callback(x); x }.logoutResult
    //    case c: SendAudio => sendAudio(c).map { x => callback(x); x }.logoutResult
    case c: SendChatAction => sendChatAction(c).map { x => callback(x); x }.logoutResult
    case c: SendDocument => sendDocument(c).map { x => callback(x); x }.logoutResult
    case c: SendLocation => sendLocation(c).map { x => callback(x); x }.logoutResult
    case c: SendMessage => sendMessage(c).map { x => callback(x); x }.logoutResult
    case c: DeleteMessage => deleteMessage(c).map { x => callback(x); x }.logoutResult
    //    case c: SendSticker => sendSticker(c).map { x => callback(x); x }.logoutResult
    //    case c: SendVideo => sendVideo(c).map { x => callback(x); x }.logoutResult
    //    case c: SendVoice => sendVoice(c).map { x => callback(x); x }.logoutResult
    case c: ForwardMessage => forwardMessage(c).map { x => callback(x); x }.logoutResult
    case c: GetFile => getFile(c).map { x => callback(x); x }.logoutResult
    case c: GetUserProfilePhotos => getUserProfilePhotos(c).map { x => callback(x); x }.logoutResult
  }

  override implicit def actorSystem: ActorSystem = context.system
}
