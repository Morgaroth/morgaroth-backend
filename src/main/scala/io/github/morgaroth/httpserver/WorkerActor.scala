package io.github.morgaroth.httpserver

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.github.morgaroth.base._
import io.github.morgaroth.httpserver.socketio.SessionRegistryActor.Message
import io.github.morgaroth.httpserver.socketio.SocketIOSessionHandler


class WorkerActor(sessionId: UUID, socket: ActorRef) extends Actor with ActorLogging with MMarshalling {
  log.info(s"Worker created for $sessionId")

  override def postStop(): Unit = {
    log.error("!!Worker Died!!")
  }

  context.system.eventStream.subscribe(self, classOf[SSE])

  def SSEMsg(s: SSE) = Message(MJson.write(List("SSE", s)))

  def receive: Receive = {
    case Message(CommandBB(GetCommandsList)) =>
      sender() ! SSEMsg(SSData("Commands", CommandBB.deserializers.keySet))
    case Message(CommandBB(cmd)) =>
      context.system.eventStream.publish(cmd)
    case e: EventLog =>
      socket ! Message(MJson.write(List("ServerEvent", e)))
    case s: SSData =>
      socket ! SSEMsg(s)
    case msg =>
      log.error(s"Worker received unknown $msg - $sender")
  }
}

object WorkerActor extends SocketIOSessionHandler {
  override def sessionHandler(sessionId: UUID, socket: ActorRef) = Props(new WorkerActor(sessionId, socket))
}