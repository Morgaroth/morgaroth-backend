package io.github.morgaroth.gpbettingleague

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import io.github.morgaroth.base._

object SpotifyRipperActor extends ServiceManager {
  override def initialize(system: ActorSystem) = {
    system.actorOf(Props(new SpotifyRipperActor))
  }
}

class SpotifyRipperActor extends Actor with ActorLogging with LogPublisher {
  context.system.eventStream.subscribe(self, classOf[RipPlaylist])

  var lastCreds: Option[UserCredentials] = None

  override def receive = {
    case RipPlaylist(uri, Some(auth)) =>
      lastCreds = Some(auth)
      publishLog("TODO: Implement ripping actions.")

    case RipPlaylist(uri, None) if lastCreds.isDefined =>
      publishLog("TODO: Implement ripping actions.")

    case RipPlaylist(_, _) =>
      publishLog("No previous auth.")
  }

  override def logSourceName = "Sporitfy Ripper"
}