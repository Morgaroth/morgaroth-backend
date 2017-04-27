package io.github.morgaroth.base

import akka.actor.Actor

/**
  * Created by PRV on 27.04.2017.
  */
trait LogPublisher {
  this: Actor =>

  def logSourceName: String

  def publishLog(message: String) = context.system.eventStream.publish(EventLog(logSourceName, message))
}
