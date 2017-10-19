package io.github.morgaroth.photomanager

import akka.actor.Props
import io.github.morgaroth.base._

object PhotoManagerActor extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(new PhotoManagerActor))
  }
}

class PhotoManagerActor extends MorgarothActor {
  context.system.eventStream.subscribe(self, classOf[PhotoManagerCommands])

  override def receive = {
    case PhotoPing(password) =>
      publishLog(s"Confirmed with pass $password.")
  }

  override def logSourceName = "Photos"
}