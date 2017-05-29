package io.github.morgaroth.gpbettingleague

import akka.actor.Props
import com.typesafe.config.ConfigFactory
import io.github.morgaroth.base._
import org.joda.time.DateTime

object GPBettingLeagueActor extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(new GPBettingLeagueActor))
  }
}

class GPBettingLeagueActor extends MorgarothActor {
  subscribe(classOf[GPBettingCommands])

  var creds: Option[UserCredentials] = None

  val cfg = ConfigFactory.load().getConfig("gp-betting-league")

  override def receive = {
    case RunGPBettingLeague(Some(credentials: UserCredentials), _, timeBarrier) =>
      new Main(cfg).run(credentials, timeBarrier)
      creds = Some(credentials)
      publishLog("Selections made.")

    case RunGPBettingLeague(_, Some(true), timeBarrier) if creds.isDefined =>
      creds.foreach { credentials =>
        try {
          withRunner(_.run(credentials, Some(DateTime.now.plusDays(2).withTimeAtStartOfDay())))
          publishLog("Selections made using previous password.")
        } catch {
          case t: Throwable =>
            log.warning("encountered exception {} ({}) wile running RunGPBettingLeague", t.getMessage, t.getClass.getCanonicalName)
            publishLog("Error during making selections.")
        }
      }

    case RunGPBettingLeague(_, Some(true), _) if creds.isEmpty =>
      publishLog("No previous password.")

    case SaveGPCredentials(credentials) =>
      creds = Some(credentials)
      publishLog("Credentials saved.")

    case RunGPBettingLeagueTomorrowPreviousPass if creds.isDefined =>
      creds.foreach { credentials =>
        try {
          withRunner(_.run(credentials, Some(DateTime.now.plusDays(2).withTimeAtStartOfDay())))
        } catch {
          case t: Throwable =>
            publishLog("Error during making selections.")
            log.warning("encountered exception {} ({}) wile running RunGPBettingLeagueTomorrowPreviousPass", t.getMessage, t.getClass.getCanonicalName)
        }
      }
    case RunGPBettingLeagueTomorrowPreviousPass =>
      publishLog("No previous password for automatic Tomorrow betting.")
  }

  def withRunner(fn: Main => Unit) = {
    val runner = new Main(cfg)
    fn(runner)
    runner.shutdown()
  }

  override def postStop() = {
    super.postStop()
    //    new Main(cfg).shutdown()
  }

  override def logSourceName = "GP Betting"
}