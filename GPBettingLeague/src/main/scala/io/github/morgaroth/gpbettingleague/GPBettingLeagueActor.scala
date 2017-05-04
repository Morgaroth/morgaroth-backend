package io.github.morgaroth.gpbettingleague

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import io.github.morgaroth.base.{GPBettingCommands, LogPublisher, RunGPBettingLeague, ServiceManager}

object GPBettingLeagueActor extends ServiceManager {
  override def initialize(system: ActorSystem) = {
    system.actorOf(Props(new GPBettingLeagueActor))
  }
}

class GPBettingLeagueActor extends Actor with ActorLogging with LogPublisher {
  context.system.eventStream.subscribe(self, classOf[GPBettingCommands])

  var lastPassword: Option[String] = None

  override def receive = {
    case RunGPBettingLeague(Some(password), _) =>
      Main.run(password, ConfigFactory.load().getConfig("gp-betting-league"))
      lastPassword = Some(password)
      publishLog("Selections made.")

    case RunGPBettingLeague(_, Some(true)) if lastPassword.isDefined =>
      lastPassword.foreach(Main.run(_, ConfigFactory.load().getConfig("gp-betting-league")))
      publishLog("Selections made using previous password.")

    case RunGPBettingLeague(_, Some(true)) if lastPassword.isEmpty =>
      publishLog("No previous password.")
  }

  override def logSourceName = "GP Betting"
}