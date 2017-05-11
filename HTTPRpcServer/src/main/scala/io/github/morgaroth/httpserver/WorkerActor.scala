package io.github.morgaroth.httpserver

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.github.morgaroth.base._
import io.github.morgaroth.httpserver.socketio.SessionRegistryActor.Message
import io.github.morgaroth.httpserver.socketio.SocketIOSessionHandler
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write


class WorkerActor(sessionId: UUID, socket: ActorRef) extends Actor with ActorLogging {
  log.info(s"Worker created for $sessionId")

  override def postStop(): Unit = {
    log.error("!!Worker Died!!")
  }

  implicit val f = DefaultFormats

  context.system.eventStream.subscribe(self, classOf[SSE])

  def SSEMsg(s: SSE) = Message(write(List("SSE", s)))

  def receive: Receive = {
    case Message(CommandBB(GetCommandsList)) =>
      sender() ! SSEMsg(SSData("Commands", CommandBB.deserializers.keySet))
    case Message(CommandBB(cmd)) =>
      context.system.eventStream.publish(cmd)
    case e: EventLog =>
      socket ! Message(write(List("ServerEvent", e)))
    case s: SSData =>
      socket ! SSEMsg(s)
    case msg =>
      log.error(s"Worker received unknown $msg - $sender")
  }
}

object WorkerActor extends SocketIOSessionHandler {
  override def sessionHandler(sessionId: UUID, socket: ActorRef) = Props(new WorkerActor(sessionId, socket))
}