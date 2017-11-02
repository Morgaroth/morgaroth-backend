package io.github.morgaroth.telegrambot

import akka.actor.{ActorRef, Props, Stash}
import akka.event.LoggingAdapter
import com.typesafe.config.Config
import io.github.morgaroth.base._
import io.github.morgaroth.telegrambot.core.api.models.extractors._
import io.github.morgaroth.telegrambot.core.api.models.formats._
import io.github.morgaroth.telegrambot.core.api.models.{ReplyKeyboardHide, ReplyKeyboardMarkup, SendMessage, Update, User}
import io.github.morgaroth.telegrambot.core.engine.NewUpdate
import io.github.morgaroth.telegrambot.core.engine.core.BotActor.Initialized

import scala.language.reflectiveCalls

object WorkerBot {

  def props(cfg: Config) = Props(new WorkerBot(cfg))
}

class WorkerBot(cfg: Config) extends MorgarothActor with Stash {

  implicit val la: LoggingAdapter = log

  var me: User = _
  var worker = ActorRef.noSender


  override def receive: Receive = {
    case u: User =>
      me = u
      log.info(s"I'm a $u")
      context become working
      unstashAll()
      worker = sender()
    case Initialized =>
    case _ =>
      stash()
  }

  def waitingForReply(callback: String => Unit): Receive = {
    case TextCommand(text, _) =>
      callback(text)
      context.become(working)
  }

  val ALLOWED_USERS=Set(36792931, 84815764)

  def working: Receive = {
    case u@NewUpdate(_, _, Update(_, m)) if ALLOWED_USERS.contains(m.from.id) && m.chat.isPrvChat =>
      workingHandler(u)
    case u: NewUpdate =>
      log.error("Message from unknown author {}", u)
    case unhandled =>
      log.error(s"unhandled message $unhandled")
  }

  def workingHandler = PartialFunction[Any, Unit] {
    case TextCommand("/shutdown", _) =>
      sys.exit(0)

    case TextCommand("/upgrade", _) =>
      import scala.sys.process._
      log.info(s"PWD is ${"pwd".!!.trim}")
      log.info("git pull".!!.trim)

    case SingleArgCommand("_add_me", group, (chat, from, _)) if from.username.isDefined && chat.isGroupChat && group.nonEmpty =>

    case MultiArgCommand("_add", data, (chat, from, _)) if chat.isGroupChat && data.length >= 2 =>

    case TextCommand("/commands" | "cmds" | "c", (chat, _, _)) =>
      sender() ! SendMessage(chat.chatId, text = "Commands", reply_markup = ReplyKeyboardMarkup.once(
        keyboard = List(
          List("Make Selections for Tomorrow"),
          List("Make Selections"),
          List("GP Pass", "Spotify Pass"),
          List("AP On", "AP Off", "AP Status"),
          List("Hide keyboard")
        )
      ))

    case NoArgCommand("hide keyboard", (ch, _, _)) =>
      sender() ! SendMessage(ch.chatId, "closing keyboard", reply_markup = ReplyKeyboardHide())

    case SingleArgCommand("set_gp_betting_password", pass, _) =>
      publish(SaveGPCredentials(UserCredentials("mateusz.jaje", pass)))

    case SingleArgCommand("set_spotify_password", pass, _) =>
      publish(SaveSpotifyCredentials(UserCredentials("Morgaroth", pass)))

    case TextCommand("gp password" | "gp pass", (chat, _, _)) =>
      sender() ! chat.msg("Ok, now send me Your GP password")
      context.become(waitingForReply(pass => publish(SaveGPCredentials(UserCredentials("mateusz.jaje", pass)))))

    case TextCommand("spotify password" | "spotify pass", (chat, _, _)) =>
      sender() ! chat.msg("Ok, now send me Your Spotify password")
      context.become(waitingForReply(pass => publish(SaveSpotifyCredentials(UserCredentials("Morgaroth", pass)))))

    case TextCommand("ap on", _) => publish(PowerOn)
    case TextCommand("ap off", _) => publish(PowerOff)
    case TextCommand("ap status", _) => publish(AirPurifierStatus)

    case NoArgCommand("run_gp_betting_league_tomorrow", _) |
         TextCommand("run gp betting league for tomorrow" | "run gp tomorrow" | "make selections for tomorrow", _) =>
      publish(RunGPBettingLeagueTomorrowPreviousPass)

    case TextCommand("run gp betting league" | "run gp" | "make selections", _) =>
      publish(RunGPBettingLeague(None, Some(true), None))

    case TextCommand(spotifyUri, (chat, _, _)) if spotifyUri.startsWith("spotify:") =>
      publish(RipUri(spotifyUri, None))

    case NewUpdate(_, _, u: Update) if u.message.from.username.isDefined && u.message.chat.isGroupChat =>
      println(s"unhandled msg (${u.message.text}) from ${u.message.from}")

    case unhandled =>
      log.warning("unhandled message {}", unhandled)
  }
}