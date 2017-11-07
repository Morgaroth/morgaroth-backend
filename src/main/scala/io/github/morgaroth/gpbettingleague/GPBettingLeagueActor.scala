package io.github.morgaroth.gpbettingleague

import java.net.URL

import akka.actor.Props
import com.typesafe.config.ConfigFactory
import io.github.morgaroth.base._
import net.ceedubs.ficus.Ficus._
import org.joda.time.DateTime
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object GPBettingLeagueActor extends ServiceManager {
  override def initialize(ctx: MContext): Unit = {
    ctx.system.actorOf(Props(new GPBettingLeagueActor(BettingDB(ctx.cfg))))
  }
}

class GPBettingLeagueActor(db: BettingDB) extends MorgarothActor {
  subscribe(classOf[GPBettingCommands])

  private var creds: Option[UserCredentials] = None

  private val cfg = ConfigFactory.load().getConfig("gp-betting-league")

  (cfg.as[Option[String]]("username") zip cfg.as[Option[String]]("password")).foreach {
    case (username, pass) => self ! SaveGPCredentials(UserCredentials(username, pass))
  }

  context.system.scheduler.schedule(2.seconds, 6.hours, self, UpdateClosedRoundsKnowledge)

  implicit lazy val driver: RemoteWebDriver = if (cfg.hasPath("remote-server")) {
    log.info(s"running with remote server on ${cfg.getString("remote-server")}")
    new RemoteWebDriver(new URL(cfg.getString("remote-server")), DesiredCapabilities.chrome)
  } else {
    log.info(s"running with local driver ${cfg.getString("driver-path")}")
    System.setProperty("webdriver.chrome.driver", cfg.getString("driver-path"))
    try {
      new ChromeDriver()
    } catch {
      case e: RuntimeException if e.getMessage == "Unable to find a free port" =>
        import scala.sys.process._
        "pgrep chromedriver".lineStream.foreach { pid =>
          s"kill -9 $pid".!!
        }
        new ChromeDriver()
    }
  }

  override def receive: Receive = {
    case RunGPBettingLeague(Some(credentials: UserCredentials), _, timeBarrier) =>
      new Main(cfg).run(credentials, timeBarrier)
      creds = Some(credentials)
      publishLog("Selections made.")

    case RunGPBettingLeague(_, Some(true), timeBarrier) if creds.isDefined =>
      creds.foreach { credentials =>
        try {
          withRunner(_.run(credentials, timeBarrier.orElse(Some(DateTime.now.plusDays(14).withTimeAtStartOfDay()))))
          publishLog("Selections made using previous password.")
        } catch {
          case t: Throwable =>
            log.warning("encountered exception {} ({}) wile running RunGPBettingLeague", t.getMessage, t.getClass.getCanonicalName)
            t.printStackTrace()
            publishLog("Error during making selections.")
        }
      }

    case UpdateClosedRoundsKnowledge if creds.isEmpty =>
      publishLog("Set credentials firstly.")

    case UpdateClosedRoundsKnowledge =>
      try {
        withRunner { runner =>
          val action = for {
            knownRounds <- db.getRoundsKnownList
            rounds = runner.scrapRounds(creds.get)
            missingRounds = rounds.toSet -- knownRounds
            _ = log.info(s"known rounds are $knownRounds, scrapped are $rounds")
            results <- missingRounds.toList.foldLeft(Future.successful(List.empty[Option[GpRoundResult]])) { case (accF, roundId) =>
              accF.flatMap { acc =>
                val roundResult = runner.getResultsOfRound(roundId, creds.get)
                  .find(_.player == creds.get.user)
                  .map { userResult =>
                    for {
                      _ <- db.saveRound(userResult)
                      _ <- db.markRoundAsKnown(roundId)
                      _ = publishLog(s"Your result in round #$roundId is ${userResult.place}.")
                    } yield Some(userResult)
                  }.getOrElse(Future.successful(None))
                roundResult.map(_ :: acc)
              }
            }
          } yield results
          Await.result(action, 5.minutes)
        }
      } catch {
        case t: Throwable =>
          t.printStackTrace()
          publishLog("Error during checking last completed rounds.")
          log.warning("encountered exception {} ({}) wile running RunGPBettingLeagueTomorrowPreviousPass", t.getMessage, t.getClass.getCanonicalName)
      }

    case GetLastRoundResult =>
      db.getLastRoundResult.onComplete {
        case Success(x) => publishLog(x.toString)
        case Failure(_: NoSuchElementException) => publishLog("No known rounds.")
        case Failure(thr) =>
          log.error(thr, "Exception encountered during checking last known round.")
          publishLog(s"Exception encountered during checking last known round - ${thr.getMessage}")
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
            t.printStackTrace()
            publishLog("Error during making selections.")
            log.warning("encountered exception {} ({}) wile running RunGPBettingLeagueTomorrowPreviousPass", t.getMessage, t.getClass.getCanonicalName)
        }
      }
    case RunGPBettingLeagueTomorrowPreviousPass =>
      publishLog("No previous password for automatic Tomorrow betting.")
  }

  def withRunner[T](fn: Main => T) = {
    val runner = new Main(cfg)
    val result = fn(runner)
    if (result.isInstanceOf[Future[_]]) {
      log.warning("withRunner function should not return Future")
    }
    result
  }

  override def postStop() {
    super.postStop()
    driver.quit()
  }

  override def logSourceName = "GP Betting"
}