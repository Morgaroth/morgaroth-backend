package io.github.morgaroth.mongo

import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import reactivemongo.bson.{BSONDocument, BSONElement}

/**
  * Created by mateusz on 5/29/17.
  */

case class TestClass2(name: String = "test", date: DateTime = DateTime.now)

class BsonSerializationSpec extends FlatSpec with Matchers {

  val undertest = new BsonSerialization {}

  "BSON serialization of reactive mongo" should "serialize joda date to millis" in {

    val result = undertest.serialize(TestClass2())
    val fields: List[BSONElement] = result.asInstanceOf[BSONDocument].elements.toList
    fields.size shouldBe 2
    fields.find(_.value.code == 0x09.toByte) shouldBe 'defined
  }
}
