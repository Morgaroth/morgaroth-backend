package io.github.morgaroth.telegrambot

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.event.LoggingAdapter
import com.typesafe.config.Config
import io.github.morgaroth.base.{MorgarothActor, RunGPBettingLeagueTomorrowPreviousPass, SaveGPCredentials, UserCredentials}
import io.github.morgaroth.telegrambot.core.api.models.extractors._
import io.github.morgaroth.telegrambot.core.api.models.{Update, User}
import io.github.morgaroth.telegrambot.core.engine.NewUpdate

import scala.language.reflectiveCalls

object WorkerBot {

  def props(cfg: Config) = Props(classOf[WorkerBot], cfg)
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
    case _ =>
      stash()
  }

  def working: Receive = {
    case SingleArgCommand("_add_me", group, (chat, from, _)) if from.username.isDefined && chat.isGroupChat && group.nonEmpty =>

    case MultiArgCommand("_add", data, (chat, from, _)) if chat.isGroupChat && data.length >= 2 =>

    case MultiArgCommand("_add", data, (chat, from, _)) if chat.isGroupChat =>
      sender() ! chat.markupMsg("Use format: / ̲add *${non empty list of users}* *${group name}*")

    case SingleArgCommand("_remove_me", group, (chat, from, _)) if from.username.isDefined && group != "all" && chat.isGroupChat =>

    case NewChatParticipant(user, (chat, from, _)) =>
      log.info( s"""new chat participant $user in chat $chat, adding them to "all"""")

    case RemovedParticipant(user, date, (chat, from, _)) if user.username.isDefined =>
      log.info(s"removed chat participant $user from $chat")

    case SingleArgCommand("_remove_me", group, (chat, from, _)) if from.username.isDefined && chat.isGroupChat =>
      sender() ! chat.msg("You cannot remove self from all group, all is all.")

    case SingleArgCommand("set_gp_betting_password", pass, _) =>
      publish(SaveGPCredentials(UserCredentials("mateusz.jaje", pass)))

    case NoArgCommand("run_gp_betting_league_tomorrow", _) | TextCommand("Run GP Betting League For Tomorrow", _) =>
      publish(RunGPBettingLeagueTomorrowPreviousPass)

    case NewUpdate(_, _, u: Update) if u.message.from.username.isDefined && u.message.chat.isGroupChat =>
      println(s"msg from ${u.message.from}")

    case unhandled =>
      log.warning("unhandled message {}", unhandled)
  }

  val help =
    """
      |CallOutBot
      |
      |Bot for aliasing group of people in Telegram chat, allows to call them out
      |using group name/alias.
      |
      |Groups are created dynamically, by first person, who sign in.
      |Members can
      | - sign in to group by self (_add_me *group*)
      | - also they can be signed in by others (/_add *members...* *group*)
      |Everyone can signout from group by command /_remove_me *group*
      |
      |There is one special group: *all* from this one you cannot sign out.
      |
      |Since bots haven't access to chat participants list,
      |chat members have to sign in to groups, even *all* group,
      |but there is internal mechanism gro *all*: by send any message to bot,
      |user will be signed in to *all*.
      |
      |You can check list of groups in channel by /_list_groups
      |You can check members of group by /_members *group name*
      |
      |Using groups:
      |simply start you message to group by /groupname and follow your message
      |example:
      |/all can anybody help me?
      |
      |Enjoy!
    """.stripMargin
}
