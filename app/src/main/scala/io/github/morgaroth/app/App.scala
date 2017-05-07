package io.github.morgaroth.app

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.github.morgaroth.base.ServiceManager
import org.reflections.Reflections

import scala.collection.JavaConverters._
import scala.reflect._

object App {
  def main(args: Array[String]): Unit = {
    alternativeMain()
  }

  private def baseMain() {
    def companionObj[T](name: String)(implicit man: Manifest[T]) = {
      val c = Class.forName(name + "$")
      c.getField("MODULE$").get(c).asInstanceOf[ServiceManager]
    }

    val system = ActorSystem("morgaroth")
    ConfigFactory.load().getStringList("managers").asScala.toList.foreach { className =>
      companionObj(className).initialize(system)
    }
  }

  def alternativeMain() {
    val system = ActorSystem("morgaroth")
    val reflection = new Reflections("io.github.morgaroth")
    val implementations = reflection.getSubTypesOf(classOf[ServiceManager]).asScala
    implementations.foreach { clazz =>
      clazz.getField("MODULE$").get(clazz).asInstanceOf[ServiceManager].initialize(system)
    }
  }
}
