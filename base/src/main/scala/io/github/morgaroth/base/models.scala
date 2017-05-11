package io.github.morgaroth.base

import io.github.morgaroth.SealedClasses
import org.joda.time.DateTime

/** Common models */

class Subscribe(eventName: String, manifest: Manifest[_])

sealed trait SSE

case class EventLog(source: String, message: String, ts: DateTime = DateTime.now) extends SSE

case class SSData(name: String, value: AnyRef) extends SSE

sealed trait Commands {
  def name: String = getClass.getSimpleName
}

/** Internal models */
case object GetCommandsList extends Commands

/** GPBettingLeague models */

sealed trait GPBettingCommands extends Commands

case class RunGPBettingLeague(
                               password: Option[String] = None,
                               usePrevious: Option[Boolean] = None
                             ) extends GPBettingCommands

/** Photo Manager models */

sealed trait PhotoManagerCommands extends Commands

case class PhotoPing(password: String) extends PhotoManagerCommands

case class CheckConnectedDevices() extends PhotoManagerCommands

/** Spotify Manager models */

sealed trait SpotifyManagerCommands extends Commands

sealed trait SpotifyRipperCommands extends SpotifyManagerCommands

case class UserCredentials(user: String, password: String) extends SpotifyManagerCommands

case class SaveCredentials(creds: UserCredentials) extends SpotifyRipperCommands

case class RipUri(uri: String, auth: Option[UserCredentials]) extends SpotifyRipperCommands

case class AddUriToMaintain(uri: String) extends SpotifyRipperCommands

/** Internal Cron models */

sealed trait CronCommands extends Commands

case object GetEntries extends CronCommands

case class AddEntry(jobName: String, defStr: String, cmd: String) extends CronCommands

case class RemoveEntry(jobName: String) extends CronCommands

case class UpdateEntry(jobName: String, defStr: Option[String] = None, cmd: Option[String] = None) extends CronCommands

object Cmds {
  val commandManifests = SealedClasses.values[Commands]
}