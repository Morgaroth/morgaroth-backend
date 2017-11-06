package io.github.morgaroth.mongo

import io.github.morgaroth.base.FutureHelpers._
import io.github.morgaroth.base.configuration.SimpleConfig
import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.bson._

import scala.concurrent.Future

case class StringValue(key: String, value: String)

case class StringArray(key: String, values: Set[String])

case class IntArray(key: String, values: Set[Int])

case class IntValue(key: String, value: Int)

class MongoConfigProvider(mongoUri: String) extends SimpleConfig with BsonSerialization {
  val driver = MongoDriver()
  val connection: MongoConnection = driver.connection(mongoUri).get
  val connectionF: Future[MongoConnection] = future(connection)

  import connection.actorSystem.dispatcher

  val col: Future[BSONCollection] = connectionF.flatMap(_.database("Morgaroth")).map(_.collection("configuration"))


  implicit val stringHandler: BSONDocumentHandler[StringValue] = Macros.handler[StringValue]
  implicit val stringArrHandler: BSONDocumentHandler[StringArray] = Macros.handler[StringArray]
  implicit val intArrHandler: BSONDocumentHandler[IntArray] = Macros.handler[IntArray]
  implicit val intHandler: BSONDocumentHandler[IntValue] = Macros.handler[IntValue]

  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(time: BSONDateTime) = new DateTime(time.value)

    def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
  }

  override def getString(key: String): Future[String] = {
    col.flatMap(_.find(keyquery(key)).requireOne[StringValue]).map(_.value)
  }

  override def putString(key: String, value: String): Future[String] = {
    col.flatMap(_.findAndUpdate(keyquery(key), StringValue(key, value), upsert = true).map(_ => value))
  }

  private def keyquery(key: String) = {
    BSONDocument("key" -> key)
  }

  override def getAllKeys: Future[Set[String]] = col.flatMap(
    _.find(BSONDocument.empty, BSONDocument("key" -> 1)).cursor[BSONDocument]()
      .collect[Set]()).map(_.map(_.getAs[String]("key").get))

  override def put[T <: AnyRef](key: String, value: T)(implicit m: Manifest[T]): Future[T] = {
    val document = BSONDocument("key" -> key, "value" -> serialize(value))
    col.flatMap(_.findAndUpdate(keyquery(key), document, upsert = true).map(_ => value))
  }

  override def get[T <: AnyRef](key: String)(implicit m: Manifest[T]): Future[T] = {
    col.flatMap(_.find(keyquery(key)).requireOne[BSONDocument]).map(_.getTry("value").map(bson2json).map(_.extract[T])).flatMap(Future.fromTry)
  }

  override def remove(key: String): Future[Unit] = {
    col.flatMap(_.remove(keyquery(key))).map(_ => ())
  }

  override def appendToStringArray(key: String, value: String): Future[Set[String]] = {
    col.flatMap(_.findAndUpdate(keyquery(key), BSONDocument("$set" -> BSONDocument("key" -> key), "$addToSet" -> BSONDocument("values" -> value)), upsert = true).flatMap(_ => getStringArray(key)))
  }

  override def appendToIntArray(key: String, value: Int): Future[Set[Int]] = {
    col.flatMap(_.findAndUpdate(keyquery(key), BSONDocument("$set" -> BSONDocument("key" -> key), "$addToSet" -> BSONDocument("values" -> value)), upsert = true).flatMap(_ => getIntArray(key)))
  }

  override def getStringArray(key: String): Future[Set[String]] = col.flatMap(_.find(keyquery(key)).requireOne[StringArray].map(_.values))

  override def getIntArray(key: String): Future[Set[Int]] = col.flatMap(_.find(keyquery(key)).requireOne[IntArray].map(_.values))

  override def removeFromStringArray(key: String, value: String): Future[Unit] = {
    col.flatMap(_.findAndUpdate(keyquery(key), BSONDocument("$pull" -> BSONDocument("values" -> value))).map(_ => ()))
  }
}