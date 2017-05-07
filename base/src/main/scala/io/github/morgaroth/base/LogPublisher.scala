package io.github.morgaroth.base

import akka.actor.Actor

/**
  * Created by PRV on 27.04.2017.
  */
trait LogPublisher {
  this: Actor =>

  def logSourceName: String = ???

  def publish(msg: AnyRef) = {
    context.system.eventStream.publish(msg)
  }

  def publishLog(name: String, description: String): Unit = {
    publish(EventLog(name, description))
  }

  def publishLog(message: String): Unit = publishLog(logSourceName, message)
}
