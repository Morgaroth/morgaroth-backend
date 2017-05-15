package io.github.morgaroth.base

import akka.actor.{Actor, ActorSystem}
import org.json4s.JsonAST.JObject

/**
  * Created by PRV on 27.04.2017.
  */
trait MessagesPublisher {

  def logSourceName: String = ???

  def publishLog(name: String, description: String)(implicit as: ActorSystem): Unit = {
    publish(EventLog(name, description))
  }

  def publishLog(message: String)(implicit as: ActorSystem): Unit = publishLog(logSourceName, message)

  def sendToClient(name: String, data: AnyRef)(implicit as: ActorSystem): Unit = publish(SSData(name, data))

  def sendToClient(name: String)(implicit as: ActorSystem): Unit = sendToClient(name, JObject())

  def publish(msg: AnyRef)(implicit as: ActorSystem) = {
    as.eventStream.publish(msg)
  }
}