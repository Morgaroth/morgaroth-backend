package io.github.morgaroth.app

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.github.morgaroth.base.ServiceManager

import scala.collection.JavaConverters._
import scala.reflect._

object App {

  def companionObj[T](name: String)(implicit man: Manifest[T]) = {
    val c = Class.forName(name + "$")
    c.getField("MODULE$").get(c).asInstanceOf[ServiceManager]
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("morgaroth")
    ConfigFactory.load().getStringList("managers").asScala.toList.foreach { className =>
      companionObj(className).initialize(system)
    }
  }
}
