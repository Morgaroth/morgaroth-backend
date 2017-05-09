package io.github.morgaroth.spotifymanager

import akka.actor.{Actor, ActorLogging, Props}
import io.github.morgaroth.base._

object SpotifyRipperActor extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(new SpotifyRipperActor(ctx)))
  }
}

class SpotifyRipperActor(ctx: ConfigProvider) extends Actor with ActorLogging with LogPublisher {

  import scala.sys.process._

  val fileNameFormat = "{track_name} - {artist} ({album}).{ext}"
  val keyFile = ""

  context.system.eventStream.subscribe(self, classOf[SpotifyRipperCommands])
  var lastCreds: Option[UserCredentials] = None

  def rip(ent: SEntity, user: String, pass: String) = {
    s"""spotify-ripper
       | --user $user
       | --password $pass
       | --key $keyFile
       | --format "$fileNameFormat"
       | --play-token-resume 5m
       | ${ent.uri}""".stripMargin.replaceAll("( {2,})|\n", "").!!
  }

  override def receive = {
    case RipPlaylist(SUri(uri), Some(auth)) =>
      lastCreds = Some(auth)
      rip(uri, auth.user, auth.password)
      publishLog("TODO: Implement ripping actions.")

    case RipPlaylist(SUri(uri), None) if lastCreds.isDefined =>
      rip(uri, lastCreds.get.user, lastCreds.get.password)
      publishLog("TODO: Implement ripping actions.")

    case e@AddUriToMaintain(uri: String) =>
      log.info(s"received command $e")
      ctx.configProvider.appendToStringArray("spotify-ripper.stored-uris", uri)
      publishLog("Uri Saved.")

    case RipPlaylist(_, _) =>
      publishLog("No previous auth.")
  }

  override def logSourceName = "Sporitfy Ripper"
}