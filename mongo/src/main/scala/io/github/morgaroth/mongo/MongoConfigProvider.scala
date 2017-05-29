package io.github.morgaroth.mongo

import io.github.morgaroth.base.FutureHelpers._
import io.github.morgaroth.base.MMarshalling
import io.github.morgaroth.base.configuration.SimpleConfig
import org.joda.time.DateTime
import org.json4s.JsonAST._
import org.json4s.{DefaultFormats, Extraction}
import org.mongodb.scala.bson.BsonTransformer
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, _}

import scala.concurrent.Future
import scala.util.Success

case class StringValue(key: String, value: String)

case class StringArray(key: String, values: Set[String])

case class IntValue(key: String, value: Int)

class MongoConfigProvider(mongoUri: String) extends SimpleConfig with BsonSerialization {
  val driver = MongoDriver()
  val connection = driver.connection(mongoUri).get
  val connectionF = future(connection)

  import connection.actorSystem.dispatcher

  val col: Future[BSONCollection] = connectionF.flatMap(_.database("Morgaroth")).map(_.collection("configuration"))


  implicit val stringHandler = Macros.handler[StringValue]
  implicit val stringArrHandler = Macros.handler[StringArray]
  implicit val intHandler = Macros.handler[IntValue]

  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(time: BSONDateTime) = new DateTime(time.value)

    def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
  }

  override def getString(key: String) = {
    col.flatMap(_.find(keyquery(key)).requireOne[StringValue]).map(_.value)
  }

  override def putString(key: String, value: String) = {
    col.flatMap(_.findAndUpdate(keyquery(key), StringValue(key, value), upsert = true).map(_ => value))
  }

  private def keyquery(key: String) = {
    BSONDocument("key" -> key)
  }

  override def getAllKeys = col.flatMap(
    _.find(BSONDocument.empty, BSONDocument("key" -> 1)).cursor[BSONDocument]()
      .collect[Set]()).map(_.map(_.getAs[String]("key").get))

  override def put[T <: AnyRef](key: String, value: T)(implicit m: Manifest[T]) = {
    val document = BSONDocument("key" -> key, "value" -> serialize(value))
    col.flatMap(_.findAndUpdate(keyquery(key), document, upsert = true).map(_ => value))
  }

  override def get[T <: AnyRef](key: String)(implicit m: Manifest[T]) = {
    col.flatMap(_.find(keyquery(key)).requireOne[BSONDocument]).map(_.getTry("value").map(bson2json).map(_.extract[T])).flatMap(Future.fromTry)
  }

  override def remove(key: String): Future[Unit] = {
    col.flatMap(_.remove(keyquery(key))).map(_ => ())
  }

  override def appendToStringArray(key: String, value: String) = {
    col.flatMap(_.findAndUpdate(keyquery(key), BSONDocument("$set" -> BSONDocument("key" -> key), "$addToSet" -> BSONDocument("values" -> value)), upsert = true).flatMap(_ => getStringArray(key)))
  }

  override def getStringArray(key: String) = col.flatMap(_.find(keyquery(key)).requireOne[StringArray].map(_.values))

  override def removeFromStringArray(key: String, value: String): Future[Unit] = {
    col.flatMap(_.findAndUpdate(keyquery(key), BSONDocument("$pull" -> BSONDocument("values" -> value))).map(_ => ()))
  }
}