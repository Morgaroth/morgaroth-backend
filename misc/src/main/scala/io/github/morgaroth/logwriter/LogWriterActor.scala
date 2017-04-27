package io.github.morgaroth.logwriter

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import io.github.morgaroth.base.{EventLog, PhotoManagerCommands, PhotoPing, ServiceManager}

object LogWriterActor extends ServiceManager {
  override def initialize(system: ActorSystem) = {
    system.actorOf(Props(new LogWriterActor))
  }
}

class LogWriterActor extends Actor with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[EventLog])

  override def receive = {
    case EventLog(source, msg, _) =>
      log.info(s"Event: $source - $msg")
  }
}