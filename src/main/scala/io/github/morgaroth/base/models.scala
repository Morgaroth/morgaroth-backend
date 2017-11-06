package io.github.morgaroth.base

import io.github.morgaroth.SealedClasses
import org.joda.time.DateTime

/** Common models */

class Subscribe(eventName: String, manifest: Manifest[_])

case class UserCredentials(user: String, password: String)

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
                               credentials: Option[UserCredentials] = None,
                               usePrevious: Option[Boolean] = None,
                               newerThan: Option[DateTime] = None,
                             ) extends GPBettingCommands

case object RunGPBettingLeagueTomorrowPreviousPass extends GPBettingCommands

case class SaveGPCredentials(creds: UserCredentials) extends GPBettingCommands

case object UpdateClosedRoundsKnowledge extends GPBettingCommands

case object GetLastRoundResult extends GPBettingCommands

/** Photo Manager models */

sealed trait PhotoManagerCommands extends Commands

case class PhotoPing(password: String) extends PhotoManagerCommands

case class CheckPhoneConnected() extends PhotoManagerCommands

case class CheckExternalDriveConnected() extends PhotoManagerCommands

/** Spotify Manager models */

sealed trait SpotifyManagerCommands extends Commands

sealed trait SpotifyRipperCommands extends SpotifyManagerCommands

case class SaveSpotifyCredentials(creds: UserCredentials) extends SpotifyRipperCommands

case class RipUri(uri: String, auth: Option[UserCredentials]) extends SpotifyRipperCommands

case class AddUriToMaintain(uri: String) extends SpotifyRipperCommands

/** Internal Cron models */

sealed trait CronCommands extends Commands

case object GetEntries extends CronCommands

case class AddEntry(jobName: String, defStr: String, cmd: String) extends CronCommands

case class RemoveEntry(jobName: String) extends CronCommands

case class UpdateEntry(jobName: String, defStr: Option[String] = None, cmd: Option[String] = None) extends CronCommands

/** Air Purifier commands */
sealed trait AirPurifierCommands

case object PowerOn extends AirPurifierCommands

case object PowerOff extends AirPurifierCommands

case object AirPurifierStatus extends AirPurifierCommands

case class SetOperateMode(mode: String) extends AirPurifierCommands

case class SetFavoriteLevel(level: Int) extends AirPurifierCommands

case class SetLedBrightness(volume: String) extends AirPurifierCommands

object Cmds {
  val commandManifests = SealedClasses.values[Commands]
}