package io.github.morgaroth.base

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingAdapter

/**
  * Created by PRV on 06.05.2017.
  */
trait MorgarothActor extends Actor with ActorLogging {

  implicit val implicitLogger: LoggingAdapter = log

  def publish(msg: AnyRef) = {
    context.system.eventStream.publish(msg)
  }

  def publishLog(name: String, description: String) = {
    publish(EventLog(name, description))
  }
}
