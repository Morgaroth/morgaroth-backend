package io.github.morgaroth.telegrambot

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.{Config, ConfigFactory}
import io.github.morgaroth.base._
import io.github.morgaroth.telegrambot.core.api.models.SendMessage
import io.github.morgaroth.telegrambot.core.engine.core.BotActor
import io.github.morgaroth.telegrambot.core.engine.pooling.LongPoolingActor
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

case class BotConfig(
                      botName: String,
                      botToken: String,
                      more: Option[Config]
                    ) {
  def additional: Config = more.getOrElse(ConfigFactory.empty())
}

object TelegramIntegration extends ServiceManager {
  override def initialize(ctx: MContext): Unit = {
    ctx.system.actorOf(Props(new TelegramIntegration))
  }
}

class TelegramIntegration extends Actor with ActorLogging with MessagesPublisher {
  context.system.eventStream.subscribe(self, classOf[EventLog])

  val sett: BotConfig = ConfigFactory.load.as[BotConfig]("telegram-bot")

  private val updatesProvider = context.actorOf(LongPoolingActor.props(sett.botName, sett.botToken), s"${sett.botName}-long-poll")
  private val worker = context.actorOf(WorkerBot.props(sett.additional))

  private val botActor = context.actorOf(BotActor.props(sett.botName, sett.botToken, updatesProvider, worker), s"${sett.botName}-bot")

  override def receive: Receive = {
    case EventLog(s, msg, _) =>
      botActor ! SendMessage(36792931, s"$s: $msg")
    case unhandled =>
      log.warning("unhandled {}", unhandled)
  }

  override def logSourceName = "Telegram Integration"
}