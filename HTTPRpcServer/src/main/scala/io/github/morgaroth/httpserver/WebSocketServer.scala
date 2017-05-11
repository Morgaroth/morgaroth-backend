package io.github.morgaroth.httpserver

import akka.actor.{ActorLogging, FSM, Props}
import akka.http.scaladsl.Http
import io.github.morgaroth.base.{EventLog, MContext, ServiceManager}
import io.github.morgaroth.httpserver.WebSocketServer.{Data, State}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by PRV on 23.04.2017.
  */
//@formatter:off
object WebSocketServer extends ServiceManager {

  def initialize(ctx: MContext) = {
    val ref = ctx.system.actorOf(Props(new WebSocketServer))
    ref ! Connect(ctx.staticCfg.getString("ws.interface"), ctx.staticCfg.getInt("ws.port"))
  }

  sealed trait State
  case object Working extends State
  case object NotWorking extends State

  sealed trait Data
  case object Uninitialized extends Data
  case class Initialized(interface: String, port: Int, binding: Future[Http.ServerBinding]) extends Data

  case class Connect(interface: String, port: Int)
  case class Disconnect(interface: String, port: Int)
  case object Hi
}
//@formatter:on

class WebSocketServer extends FSM[State, Data] with ActorLogging {

  import context.dispatcher
  import io.github.morgaroth.httpserver.WebSocketServer._

  when(NotWorking) {
    case Event(Connect(interface, port), Uninitialized) =>
      val api = new SocketIOServer()(context.system)
      val binding = api.bind(port, interface)
      context.system.eventStream.publish(EventLog("HTTPServer", s"Bound to $interface:$port."))
      goto(Working) using Initialized(interface, port, binding)
    case Event(hi, state) =>
      sender() ! state
      stay()
  }

  when(Working) {
    case Event(Disconnect, Initialized(_, _, binding)) =>
      Await.result(binding.flatMap(_.unbind()), 20.seconds)
      goto(NotWorking) using Uninitialized
    case Event(hi, state) =>
      sender() ! state
      stay()
  }

  startWith(NotWorking, Uninitialized)
  initialize()
}

