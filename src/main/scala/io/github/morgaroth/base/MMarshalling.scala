package io.github.morgaroth.base

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.json4s.JsonAST.{JInt, JLong, JNull, JString}
import org.json4s.native.Serialization
import org.json4s.{CustomSerializer, DefaultFormats, MappingException, native}

import scala.util.Try

/**
  * Created by PRV on 24.05.2017.
  */
trait MMarshalling {

  implicit val formats = DefaultFormats + JodaDateSerializer

  implicit val serializationLibrary: org.json4s.Serialization = native.Serialization

  val MJson = Serialization
}

object JodaDateSerializer extends CustomSerializer[DateTime](format => ( {
  case JString(s) =>
    DateSerializer.read(s).getOrElse {
      throw new MappingException(s"Can't read date from string\'$s\'")
    }
  case JInt(d) => new DateTime(d.longValue)
  case JLong(d) => new DateTime(d)
  case JNull => null
}, {
  case d: DateTime => JString(DateSerializer.write(d))
}
))

object DateSerializer {
  val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")

  def write(d: DateTime) = formatter.print(d)

  def read(str: String) = Try(formatter.parseDateTime(str))
}
