package io.github.morgaroth.internalcron

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import io.github.morgaroth.base.{EventLog, PhotoManagerCommands, PhotoPing, ServiceManager}

object InternalCron extends ServiceManager {
  override def initialize(system: ActorSystem) = {
    system.actorOf(Props(new InternalCron))
  }
}

class InternalCron extends Actor with ActorLogging {

  override def receive = {
    case _ =>
  }
}