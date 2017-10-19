package io.github.morgaroth.spotifymanager

/**
  * Created by PRV on 08.05.2017.
  */

trait SEntity {
  def id: String

  def isPlaylist = false

  def isUser = false

  def isTrack = false

  def uri: String
}

case class STrack(id: String) extends SEntity {
  override val isTrack = true

  lazy val uri: String = s"spotify:track:$id"
}

case class SPlaylist(id: String, owner: String) extends SEntity {
  override def isPlaylist = true

  lazy val uri: String = s"spotify:user:$owner:playlist:$id"
}

case class SUser(id: String) extends SEntity {
  override def isUser = true

  lazy val uri: String = s"spotify:user:$id"
}

object SUri {
  def unapply(arg: String): Option[SEntity] = {
    // user's playlist: spotify:user:morgaroth:playlist:0WLpg4SQRki9IpCyZwPprd
    // track: spotify:track:0XuLxJ1N5A0ghDMgheyHg4
    // user: spotify:user:morgaroth
    arg.split(":").toList match {
      case "spotify" :: "track" :: trackId :: Nil => Some(STrack(trackId))
      case "spotify" :: "user" :: userName :: "playlist" :: playlistId :: Nil => Some(SPlaylist(playlistId, userName))
      case "spotify" :: "user" :: userName :: Nil => Some(SUser(userName))
      case _ => None
    }
  }
}

object STrack {
  def unapply(arg: String): Option[SEntity] = {
    // track: spotify:track:0XuLxJ1N5A0ghDMgheyHg4
    arg.split(":").toList match {
      case "spotify" :: "track" :: trackId :: Nil => Some(STrack(trackId))
      case _ => None
    }
  }
}

object SPlaylist {
  def unapply(arg: String): Option[SEntity] = {
    // user's playlist: spotify:user:morgaroth:playlist:0WLpg4SQRki9IpCyZwPprd
    arg.split(":").toList match {
      case "spotify" :: "user" :: userName :: "playlist" :: playlistId :: Nil => Some(SPlaylist(playlistId, userName))
      case _ => None
    }
  }
}

object SUser {
  def unapply(arg: String): Option[SEntity] = {
    // user: spotify:user:morgaroth
    arg.split(":").toList match {
      case "spotify" :: "user" :: userName :: Nil => Some(SUser(userName))
      case _ => None
    }
  }
}
