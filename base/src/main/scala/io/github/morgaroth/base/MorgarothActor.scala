package io.github.morgaroth.base

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingAdapter

/**
  * Created by PRV on 06.05.2017.
  */
trait MorgarothActor extends Actor with ActorLogging with LogPublisher {

  implicit val implicitLogger: LoggingAdapter = log

  def subscribe(channel: Class[_]) =
    context.system.eventStream.subscribe(self, channel)

  implicit val implicirExecContext = context.system.dispatcher
}
