package io.github.morgaroth.base

import org.joda.time.DateTime

class Subscribe(eventName: String, manifest: Manifest[_])

trait GPBettingCommands

case class RunGPBettingLeague(
                               password: Option[String] = None,
                               usePrevious: Option[Boolean] = None
                             ) extends GPBettingCommands

case class EventLog(source: String, message: String, ts: DateTime = DateTime.now)

trait PhotoManagerCommands

case class PhotoPing(password: String) extends PhotoManagerCommands