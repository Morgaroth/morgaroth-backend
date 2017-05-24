package io.github.morgaroth.base

import org.json4s.JsonAST.{JArray, JString, JValue}

object Command extends MMarshalling {
  def unapply(arg: String): Option[(String, JValue)] = {
    try {
      val arr = MJson.read[JArray](arg).arr
      val (JString(name) :: value :: Nil) = arr
      Some((name, value))
    } catch {
      case _: Throwable => None
    }
  }
}

object CommandBB extends MMarshalling {
  val deserializers = Cmds.commandManifests

  private val pf = PartialFunction[String, Commands] {
    case Command(name, args) if deserializers.contains(name) =>
      args.extract(formats, deserializers(name))
  }

  def interpret(data: String): Option[Commands] = {
    pf.andThen(Some(_)).applyOrElse(data, (_: String) => None)
  }

  def unapply(arg: String): Option[Commands] = interpret(arg)
}