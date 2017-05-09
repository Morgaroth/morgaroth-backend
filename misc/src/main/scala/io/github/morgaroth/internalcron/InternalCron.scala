package io.github.morgaroth.internalcron

import akka.actor.{Actor, ActorLogging, Props}
import io.github.morgaroth.base._

object InternalCron extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(new InternalCron(ctx)))
  }
}

class InternalCron(ctx: ConfigProvider) extends Actor with ActorLogging {

  override def receive = {
    case _ =>
  }
}