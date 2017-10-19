package io.github.morgaroth.mongo

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.{DockerContainer, DockerKit, DockerPortMapping, DockerReadyChecker}
import org.bson.{BsonReader, BsonType, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.joda.time.DateTime
import org.mongodb.scala._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Suite}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries, fromCodecs}
import org.mongodb.scala.bson.{BsonTransformer, ObjectId}

import scala.concurrent.duration._

trait DockerMongoDB extends DockerKitDockerJava {
  val MongoPort = 27019

  implicit val dat = new BsonTransformer[DateTime] {
    def apply(jdtime: DateTime) = new org.bson.BsonInt64(jdtime.getMillis)

  }

  val mongodbContainer = DockerContainer("mongo:3.4.4")
    .withPortMapping(27017 -> DockerPortMapping(Some(MongoPort)))
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}

trait DockerBeforeAndAfterAll extends BeforeAndAfterAll with ScalaFutures {
  this: Suite with DockerKit =>

  private implicit val pc = PatienceConfig(5.minutes, 1.second)

  override def beforeAll() {
    super.beforeAll()
    startAllOrFail()
    dockerContainers.foreach { container =>
      println(s"Waiting for $container")
      container.isReady().futureValue
    }
  }

  override def afterAll() {
    stopAllQuietly()
    super.afterAll()
  }
}

case class KVPair(_id: ObjectId, key: String, value: TestClass)

case class TestClass(name: String, date: DateTime = DateTime.now)


object DateTimeCodec extends Codec[DateTime] {
  override def encode(writer: BsonWriter, value: DateTime, encoderContext: EncoderContext): Unit = value match {
    case d: DateTime => writer.writeDateTime(d.getMillis)
  }

  override def getEncoderClass: Class[DateTime] = classOf[DateTime]

  override def decode(reader: BsonReader, decoderContext: DecoderContext): DateTime = {
    reader.getCurrentBsonType match {
      case BsonType.DATE_TIME ⇒ DateTime.now.withMillis(reader.readDateTime())
    }
  }
}

class MongoConfigProviderSpec extends FlatSpec with Matchers with DockerMongoDB with ScalaFutures with DockerBeforeAndAfterAll {

  private implicit val pc = PatienceConfig(5.seconds, 100.millis)
  //  private implicit val pc = PatienceConfig(5.days, 1.hours)

  val codecRegistry = fromRegistries(
    fromProviders(classOf[KVPair]),
    fromProviders(classOf[TestClass]),
    fromCodecs(DateTimeCodec),
    DEFAULT_CODEC_REGISTRY,
  )

  def withDb(fn: MongoCollection[Document] => Unit) = {
    val client = MongoClient(s"mongodb://0.0.0.0:$MongoPort")
    val DB = client.getDatabase("Morgaroth")
    val testCollection = DB.getCollection("configuration")
    try {
      fn(testCollection)
    } finally {
      client.close()
    }
  }

  def withTCDb(fn: MongoCollection[KVPair] => Unit) = {
    val client = MongoClient(s"mongodb://0.0.0.0:$MongoPort")
    val DB = client.getDatabase("Morgaroth").withCodecRegistry(codecRegistry)
    val testCollection = DB.getCollection[KVPair]("configuration")
    try {
      fn(testCollection)
    } finally {
      client.close()
    }
  }

  "mongo configuration" should "save string value correctly" in withDb { col =>
    val underTest = new MongoConfigProvider(s"mongodb://0.0.0.0:$MongoPort")
    val key = "test.key"
    val value = "test.value"

    underTest.putString(key, value).futureValue
    col.find().toFuture().futureValue.size shouldBe 1
    col.drop()
  }

  it should "save case class with time correctly" in withTCDb { col =>
    val underTest = new MongoConfigProvider(s"mongodb://0.0.0.0:$MongoPort")
    val key = "test.key"
    val value = "Some name"

    underTest.put(key, TestClass(value)).futureValue
    val documents = col.find().toFuture().futureValue
    documents.size shouldBe 1

    col.drop()
  }
}