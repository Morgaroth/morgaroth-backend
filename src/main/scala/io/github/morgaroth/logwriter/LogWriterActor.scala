package io.github.morgaroth.logwriter

import akka.actor.{Actor, ActorLogging, Props}
import io.github.morgaroth.base._

object LogWriterActor extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(new LogWriterActor))
  }
}

class LogWriterActor extends Actor with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[EventLog])

  override def receive = {
    case EventLog(source, msg, _) =>
      log.info(s"Event: $source - $msg")
  }
}