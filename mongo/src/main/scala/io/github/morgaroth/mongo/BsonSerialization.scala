package io.github.morgaroth.mongo

import io.github.morgaroth.base.{DateSerializer, MMarshalling}
import org.joda.time.DateTime
import org.json4s.Extraction
import org.json4s.JsonAST._
import reactivemongo.bson.{BSONArray, BSONBoolean, BSONDateTime, BSONDocument, BSONDouble, BSONInteger, BSONLong, BSONNull, BSONObjectID, BSONString, BSONUndefined, BSONValue}

import scala.util.Success

/**
  * Created by mateusz on 5/29/17.
  */
trait BsonSerialization extends MMarshalling {
  def json2bson(v: JValue): BSONValue = v match {
    case JInt(i) => BSONInteger(i.toInt)
    case JLong(i) => BSONLong(i)
    case JString(d) if DateSerializer.read(d).isSuccess => BSONDateTime(DateSerializer.read(d).get.getMillis)
    case JString(d) => BSONObjectID.parse(d).getOrElse(BSONString(d))
    case JDouble(d) => BSONDouble(d)
    case JObject(d) => BSONDocument(d.map { case (name, valu) => name -> json2bson(valu) })
    case JArray(d) => BSONArray(d.map(json2bson))
    case JNull => BSONNull
    case JNothing => BSONUndefined
    case JBool(d) => BSONBoolean(d)
  }

  def bson2json(v: BSONValue): JValue = v match {
    case BSONInteger(i) => JInt(i.toInt)
    case BSONLong(i) => JLong(i)
    case BSONString(d) => JString(d)
    case BSONDouble(d) => JDouble(d)
    case BSONDateTime(millis) => JString(DateSerializer.write(DateTime.now.withMillis(millis)))
    case BSONNull => JNull
    case BSONUndefined => JNothing
    case BSONBoolean(d) => JBool(d)
    case d@BSONObjectID(_) => JString(d.stringify)
    case BSONDocument(d) => JObject(d.collect { case Success(elem) => elem.name -> bson2json(elem.value) }: _*)
    case BSONArray(d) => JArray(d.flatMap(_.toOption).map(bson2json).toList)
  }

  def serialize[T <: AnyRef](value: T)(implicit m: Manifest[T]): BSONValue = {
    val json = Extraction.decompose(value)(formats)
    json2bson(json)
  }
}