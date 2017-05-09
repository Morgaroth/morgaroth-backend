package io.github.morgaroth.base

import io.github.morgaroth.SealedClasses
import org.joda.time.DateTime

class Subscribe(eventName: String, manifest: Manifest[_])

case class EventLog(source: String, message: String, ts: DateTime = DateTime.now)

sealed trait Commands {
  def name: String = getClass.getSimpleName
}

sealed trait GPBettingCommands extends Commands

case class RunGPBettingLeague(
                               password: Option[String] = None,
                               usePrevious: Option[Boolean] = None
                             ) extends GPBettingCommands {
}

sealed trait PhotoManagerCommands extends Commands

case class PhotoPing(password: String) extends PhotoManagerCommands

case class CheckConnectedDevices() extends PhotoManagerCommands

sealed trait SpotifyManagerCommands extends Commands

sealed trait SpotifyRipperCommands extends SpotifyManagerCommands

case class UserCredentials(user: String, password: String) extends SpotifyManagerCommands

case class RipPlaylist(playlistUri: String, auth: Option[UserCredentials]) extends SpotifyRipperCommands

case class AddUriToMaintain(uri: String) extends SpotifyRipperCommands

object Cmds {
  val commandManifests = SealedClasses.values[Commands]
}