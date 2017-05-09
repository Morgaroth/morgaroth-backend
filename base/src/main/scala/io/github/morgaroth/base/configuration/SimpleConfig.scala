package io.github.morgaroth.base.configuration

import scala.concurrent.Future

trait SimpleConfig {

  def getString(key: String): Future[String]

  def putString(key: String, value: String): Future[String]

  def getStringArray(key: String): Future[Set[String]]

  def appendToStringArray(key: String, value: String): Future[Set[String]]

  def getAllKeys: Future[Set[String]]
}