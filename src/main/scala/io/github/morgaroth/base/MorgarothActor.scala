package io.github.morgaroth.base

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.event.LoggingAdapter

import scala.concurrent.ExecutionContext

/**
  * Created by PRV on 06.05.2017.
  */
trait MorgarothActor extends Actor with ActorLogging with MessagesPublisher {

  implicit val implicitLogger: LoggingAdapter = log
  implicit val implicitExecContext: ExecutionContext = context.system.dispatcher
  implicit val implicitActorSystem: ActorSystem = context.system

  def subscribe(channel: Class[_])(implicit s: ActorRef): Boolean =
    context.system.eventStream.subscribe(s, channel)

  val hardSelf: ActorRef = self
}
