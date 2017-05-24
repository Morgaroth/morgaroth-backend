package io.github.morgaroth.telegrambot.core.engine.pooling

import java.util.UUID

import akka.actor._
import io.github.morgaroth.telegrambot.core.api.methods.{GetUpdatesReq, Methods, Response}
import io.github.morgaroth.telegrambot.core.api.models.Update
import io.github.morgaroth.telegrambot.core.engine._

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps, reflectiveCalls}
import scala.util.{Failure, Success}

/**
  * Created by mateusz on 18.09.15.
  */
object


LongPoolingActor {

  def props(botName: String, botToken: String) =
    Props(classOf[LongPoolingActor], botName, botToken)

  implicit def wrapToMaxoption[A: Ordering](tr: TraversableOnce[A]): Object {def maxOpt: Option[A]} = new {
    def maxOpt = if (tr.isEmpty) None else Some(tr.max)
  }

  private[LongPoolingActor] case object Poll

}

class LongPoolingActor(botName: String, val botToken: String) extends Actor with ActorLogging with Methods {

  import LongPoolingActor._
  import context.dispatcher

  val hardSelf = self
  var offset: Option[Int] = None
  var botActor = context.system.deadLetters

  def dispatchPoll(): Unit = {
    hardSelf ! Poll
  }

  override def receive: Receive = registering

  def registering: Receive = {
    case Register(bn, bt, ref) if bn == botName && bt == botToken && ref == sender() =>
      log.info(s"registering BotActor ${sender()} in LongPoolActor $self")
      botActor = sender()
      context become working
      dispatchPoll()
      botActor ! Registered
    case unhandled =>
      log.warning(s"unhandled message $unhandled")
  }

  def working: Receive = {
    case UnRegister(_, _) =>
      botActor = context.system.deadLetters
      context become registering
      sender() ! Unregistered

    case Poll =>
      //      log.debug(s"dispatching poll to bot $botName")
      getUpdates(GetUpdatesReq(offset, 10, 10 seconds)).onComplete(hardSelf ! _)

    case Success(r@Response(true, Right(Nil), _)) =>
      //      log.debug(s"received empty updates list")
      dispatchPoll()

    case Success(r@Response(true, Right(updates: List[Update]), _)) =>
      log.info(s"received updates $updates")
      val nextId = updates.map(_.update_id).maxOpt.map(_ + 1)
      offset = nextId
      updates.sortBy(_.update_id).foreach(u => botActor ! NewUpdate(UUID.randomUUID(), botName, u))
      dispatchPoll()

    case Success(response: Response[List[Update]]) =>
      log.warning(s"for bot $botName another response $response")
      dispatchPoll()

    case Failure(ex) =>
      log.error(ex, s"pooling bot $botName end with exception")
      dispatchPoll()

    case unhandled =>
      log.warning(s"unhandled message $unhandled")

  }

  override def actorSystem: ActorSystem = context.system
}
