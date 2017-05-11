package io.github.morgaroth.base

import akka.actor.Actor
import org.json4s.JsonAST.{JNull, JObject}

/**
  * Created by PRV on 27.04.2017.
  */
trait MessagesPublisher {
  this: Actor =>

  def logSourceName: String = ???

  def publish(msg: AnyRef) = {
    context.system.eventStream.publish(msg)
  }

  def publishLog(name: String, description: String): Unit = {
    publish(EventLog(name, description))
  }

  def publishLog(message: String): Unit = publishLog(logSourceName, message)

  def sendToClient(name: String, data: AnyRef): Unit = publish(SSData(name, data))

  def sendToClient(name: String): Unit = sendToClient(name, JObject())
}
