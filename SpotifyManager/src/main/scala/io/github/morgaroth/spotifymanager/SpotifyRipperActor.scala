package io.github.morgaroth.spotifymanager

import akka.actor.Props
import better.files._
import io.github.morgaroth.base.FutureHelpers._
import io.github.morgaroth.base._
import io.github.morgaroth.spotifymanager.SpotifyRipperActor.{Ripped, RippingDone}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.sys.process._
import scala.util.{Failure, Success}

object SpotifyRipperActor extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(classOf[SpotifyRipperActor],ctx))
  }

  case object RippingDone

  case class Ripped(file: String)

}

class SpotifyRipperActor(ctx: ConfigProvider) extends MorgarothActor {
  subscribe(classOf[SpotifyRipperCommands])

  var lastCreds: Option[UserCredentials] = None
  var currentTask: Future[_] = _

  val fileNameFormat = "{playlist}/{artist} - {track_name} ({album}).{ext}"
  val keyFile = (File.home / "spotify_appkey.key").pathAsString

  val outputDir = (File.home / "songs").pathAsString

  def rip(ent: SEntity, user: String, pass: String) = {
    val cmd = Seq("spotify-ripper", "--user", user, "--password", pass, "--key", keyFile, "--directory", outputDir, "--format", fileNameFormat,
      "--play-token-resume=5m", ent.uri)
    log.info(s"ripping command is ${cmd.mkString(" ")}")
    Future(cmd.lineStream_!.filterNot(_.contains("Progress: [")).filterNot(_.contains("Total: [")).map(_.drop(5).dropRight(5)).map { l =>
      if (l.contains(".mp3") && l.startsWith(outputDir)) selfie ! Ripped(File(l).name)
      l
    }.toList)
  }

  override def receive = {
    case Ripped(name) =>
      publishLog(s"Song $name ripped.")
    case RippingDone =>
      currentTask = null
    case RipUri(SUri(uri), Some(auth)) if currentTask == null =>
      lastCreds = Some(auth)
      currentTask = rip(uri, auth.user, auth.password)
      currentTask.onComplete {
        case Success(_) => log.warning(s"Ripping ended.")
          selfie ! RippingDone
        case Failure(thr) =>
          selfie ! RippingDone
          log.error(thr, "Ripping ended with exception.")
      }
      publishLog("Ripping started.")
    case RipUri(SUri(uri), None) if lastCreds.isDefined =>
      rip(uri, lastCreds.get.user, lastCreds.get.password)
      publishLog("TODO: Implement ripping actions.")

    case e@AddUriToMaintain(uri: String) =>
      log.info(s"received command $e")
      ctx.cfg.appendToStringArray("spotify-ripper.stored-uris", uri).whenCompleted {
        case Success(_) => publishLog("Uri Saved.")
        case Failure(t) => publishLog(s"Storing Uri unsuccessful: ${t.getMessage}.")
      }
    case SaveCredentials(auth) =>
      log.info(s"Received save credentials command for user ${auth.user}")
      lastCreds = Some(auth)
      publishLog("User credentials updated.")
    case RipUri(_, _) =>
      publishLog("Missing auth data.")
    case msg =>
      log.warning(s"received unknown message $msg")
  }


  override def logSourceName = "Spotify Ripper"
}