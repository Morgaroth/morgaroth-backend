package io.github.morgaroth.base

import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JArray, JString, JValue}
import org.json4s.native.Serialization.read

object Command {
  implicit val formats = DefaultFormats

  def unapply(arg: String): Option[(String, JValue)] = {
    try {
      val arr = read[JArray](arg).arr
      val (JString(name) :: value :: Nil) = arr
      Some((name, value))
    } catch {
      case _: Throwable => None
    }
  }
}

object CommandBB {
  implicit val f = DefaultFormats
  val deserializers = Cmds.commandManifests

  private val pf = PartialFunction[String, Commands] {
    case Command(name, args) if deserializers.contains(name) =>
      args.extract(f, deserializers(name))
  }

  def interpret(data: String): Option[Commands] = {
    pf.andThen(Some(_)).applyOrElse(data, (_: String) => None)
  }

  def unapply(arg: String): Option[Commands] = interpret(arg)
}