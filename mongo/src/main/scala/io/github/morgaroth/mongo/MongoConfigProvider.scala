package io.github.morgaroth.mongo

import io.github.morgaroth.base.FutureHelpers._
import io.github.morgaroth.base.configuration.SimpleConfig
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, _}

import scala.concurrent.Future

case class StringValue(key: String, value: String)

case class StringArray(key: String, values: Set[String])

case class IntValue(key: String, value: Int)

class MongoConfigProvider(mongoUri: String) extends SimpleConfig {
  val driver = MongoDriver()
  val connection = driver.connection(mongoUri).get
  val connectionF = future(connection)

  import connection.actorSystem.dispatcher

  val col: Future[BSONCollection] = connectionF.flatMap(_.database("Morgaroth")).map(_.collection("configuration"))

  implicit val stringHandler = Macros.handler[StringValue]
  implicit val stringArrHandler = Macros.handler[StringArray]
  implicit val intHandler = Macros.handler[IntValue]

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

  override def appendToStringArray(key: String, value: String) = {
    col.flatMap(_.findAndUpdate(keyquery(key), BSONDocument("$set" -> BSONDocument("key" -> key), "$addToSet" -> BSONDocument("values" -> value)), upsert = true).flatMap(_ => getStringArray(key)))
  }

  override def getStringArray(key: String) = col.flatMap(_.find(keyquery(key)).requireOne[StringArray].map(_.values))
}