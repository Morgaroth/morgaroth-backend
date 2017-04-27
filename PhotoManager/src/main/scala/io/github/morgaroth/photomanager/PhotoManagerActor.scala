package io.github.morgaroth.photomanager

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import io.github.morgaroth.base.{EventLog, PhotoManagerCommands, PhotoPing, ServiceManager}

object PhotoManagerActor extends ServiceManager {
  override def initialize(system: ActorSystem) = {
    system.actorOf(Props(new PhotoManagerActor))
  }
}

class PhotoManagerActor extends Actor with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[PhotoManagerCommands])

  override def receive = {
    case PhotoPing(password) =>
      context.system.eventStream.publish(EventLog("Photos", s"Confirmed with pass $password."))
  }
}