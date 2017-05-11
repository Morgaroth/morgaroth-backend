package io.github.morgaroth.base

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingAdapter

/**
  * Created by PRV on 06.05.2017.
  */
trait MorgarothActor extends Actor with ActorLogging with MessagesPublisher {

  implicit val implicitLogger: LoggingAdapter = log

  def subscribe(channel: Class[_])(implicit s: ActorRef) =
    context.system.eventStream.subscribe(s, channel)

  implicit val implicirExecContext = context.system.dispatcher

  val selfie = self
}
