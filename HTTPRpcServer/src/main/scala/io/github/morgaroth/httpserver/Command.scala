package io.github.morgaroth.httpserver

import io.github.morgaroth.httpserver.socketio.SessionRegistryActor.Message
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JArray, JString, JValue}
import org.json4s.native.Serialization.read

object Command {
  implicit val formats = DefaultFormats

  def unapply(arg: Message): Option[(String, JValue)] = {
    val (JString(name) :: value :: Nil) = read[JArray](arg.text).arr
    Some((name, value))
  }
}
