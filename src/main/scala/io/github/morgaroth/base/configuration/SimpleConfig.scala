package io.github.morgaroth.base.configuration

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

trait SimpleConfig {

  def getString(key: String): Future[String]

  def putString(key: String, value: String): Future[String]

  def put[T <: AnyRef](key: String, value: T)(implicit m: Manifest[T]): Future[T]

  def get[T <: AnyRef](key: String)(implicit m: Manifest[T]): Future[T]

  def remove(key: String): Future[Unit]

  def getStringArray(key: String): Future[Set[String]]

  def appendToStringArray(key: String, value: String): Future[Set[String]]

  def removeFromStringArray(key: String, value: String): Future[Unit]

  def getAllKeys: Future[Set[String]]
}

class InMemoryConfig extends SimpleConfig {

  val data = scala.collection.mutable.Map.empty[String, Any]

  implicit def wrap[T](value: T): Future[T] = Future.successful(value)

  override def getString(key: String): Future[String] = get[String](key)

  override def putString(key: String, value: String): Future[String] = put(key, value)

  override def put[T <: AnyRef](key: String, value: T)(implicit m: Manifest[T]): Future[T] = {
    data.update(key, value)
    value
  }

  override def get[T <: AnyRef](key: String)(implicit m: Manifest[T]): Future[T] = {
    data.get(key).map(_.asInstanceOf[T]).map(wrap).getOrElse(Future.failed(new NoSuchElementException))
  }

  override def remove(key: String): Future[Unit] = {
    data - key
    ()
  }

  override def getStringArray(key: String): Future[Set[String]] = get[Set[String]](key)

  override def appendToStringArray(key: String, value: String): Future[Set[String]] = {
    val prev = data.get(key).map(_.asInstanceOf[Set[String]]).getOrElse(Set.empty)
    val updated = prev + value
    data.update(key, updated)
    updated
  }

  override def removeFromStringArray(key: String, value: String): Future[Unit] = {
    val prev = data.get(key).map(_.asInstanceOf[Set[String]]).getOrElse(Set.empty)
    val updated = prev - value
    data.update(key, updated)
    ()
  }

  override def getAllKeys: Future[Set[String]] = wrap(data.keySet.toSet)
}