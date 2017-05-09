package io.github.morgaroth.app

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import io.github.morgaroth.base.ServiceManager
import org.reflections.Reflections

import scala.collection.JavaConverters._

object App {
  def main(args: Array[String]): Unit = {
    alternativeMain()
  }

  def alternativeMain() {
    val ctx = new AppContext(ActorSystem("morgaroth"), ConfigFactory.load())
    val log = ctx.getLogger(this)
    val reflection = new Reflections("io.github.morgaroth")
    val implementations = reflection.getSubTypesOf(classOf[ServiceManager]).asScala
    implementations.foreach { clazz =>
      val className = clazz.getCanonicalName
      log.info(s"Initializing class {}", className)
      val obj = clazz.getField("MODULE$").get(clazz)
      obj.asInstanceOf[ServiceManager].initialize(ctx)
    }
  }
}
