package io.github.morgaroth.gpbettingleague

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import io.github.morgaroth.base.{MessagesPublisher, UserCredentials}
import org.joda.time.{DateTime, Days, Minutes}
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions

import scala.language.implicitConversions

class Main(cfg: Config, driver: RemoteWebDriver)(implicit as: ActorSystem) extends MessagesPublisher with LazyLogging {
  override def logSourceName: String = "GP Betting Runner"

  implicit val implicitlyVisibleDriver: RemoteWebDriver = driver

  def scrapRounds(creds: UserCredentials): List[Int] = {
    gp.loginToGPBettingLeague(creds)
    gp.getCompletedRounds()
  }

  def getResultsOfRound(roundId: Int, creds: UserCredentials): List[GpRoundResult] = {
    gp.loginToGPBettingLeague(creds)
    gp.getResultsOfRound(roundId)
  }

  def run(creds: UserCredentials, newerThan: Option[DateTime]) {
    publishLog("Making selections started.")

    val ocBets = oc.scrapMatches(newerThan).map(x => x.uId -> x).toMap
    publishLog("Data from oddschecker scrapped.")
    println(s"time = $newerThan")
    println(ocBets)

    gp.loginToGPBettingLeague(creds)
    logger.debug("Logged into GPBettingLeague page")

    gp.getActiveRounds().foreach { round =>
      val roundId = round.stripPrefix("https://bettingleaguegp.appspot.com/round.jsp?id=")
      val data = gp.getMatches(round).filter(x => newerThan.forall(_.isAfter(x.start)))
      lazy val betsStatistics = gp.getCurrentStatsOfMatches(roundId).flatMap(x => x.mostPopularBet.map(x.matchId -> _.forecast)).toMap
      val changes = data.map { gpMatch =>
        logger.info(s"  ${gpMatch.host} vs ${gpMatch.guest}")
        ocBets.get(gpMatch.uId) map { ocMatch =>
          logger.info(s"    Diff in minutes: ${Minutes.minutesBetween(ocMatch.start, gpMatch.start).getMinutes}")
          logger.info(s"    Odds:")
          logger.info(s"      ${ocMatch.host}: ${ocMatch.hostBet}")
          logger.info(s"      Draw: ${ocMatch.drawBet}")
          logger.info(s"      ${ocMatch.guest}: ${ocMatch.guestBet}")
          val targetScore = oddsToScore(ocMatch.hostBet, ocMatch.guestBet)
          val forecast = s"${targetScore._1}:${targetScore._2}"
          logger.info(s"      Forecast $forecast")
          makeSelectionForMatch(gpMatch, targetScore)
        } getOrElse {
          val possible = ocBets.filter(b => b._1.contains(gpMatch.guest.take(5)) || b._1.contains(gpMatch.host.take(5))).toList
          logger.info(s"\tNo match for ${gpMatch.host} vs ${gpMatch.guest} at (${gpMatch.start.toString("dd MMM HH:mm")}).")
          val daysToStart = Days.daysBetween(DateTime.now(), gpMatch.start).getDays
          if (daysToStart > 6) {
            logger.info(s"\tBut it is match in far future, $daysToStart days from now.")
          }
          logger.info(s"\tBut there are ${possible.length} options:")
          possible.foreach { poss =>
            val aliases = List((gpMatch.host, poss._2.host), (gpMatch.guest, poss._2.guest)).filter(x => x._1 != x._2).find(x => x._2.contains(x._1.take(5))).map(a =>
              s""""${a._1}" -> List("${a._2}"),"""
            ).getOrElse("No Aliases")
            logger.info(s"\t\t${poss.toString}, $aliases")
          }
          publishLog(s"No match ${gpMatch.host} vs ${gpMatch.guest} at (${gpMatch.start.toString("dd MMM HH:mm")}).")

          if (daysToStart <= 2) {
            val forecast = betsStatistics.getOrElse(gpMatch.matchId, (2, 1))
            makeSelectionForMatch(gpMatch, forecast, Some("because match begins shortly"))
          } else {
            val forecast = betsStatistics.getOrElse(gpMatch.matchId, (3, 1))
            makeSelectionForMatch(gpMatch, forecast, Some("match unknown in oddschecker"))
          }
        }
      }.sum
      if (changes > 0) {
        val submit = gp.getSaveButton()
        submit.click()
        Option(ExpectedConditions.alertIsPresent()(driver)).foreach { _ =>
          driver.switchTo().alert().accept()
        }
        publishLog(s"Selections for round #$roundId made.\n$round")
      } else {
        publishLog(s"No matches to make selections for round #$roundId.\n$round")
      }
    }
    driver.close()
  }

  def makeSelectionForMatch(gpMatch: GpMatch, targetScore: (Int, Int), reason: Option[String] = None): Int = {
    val previousSelection = gpMatch.currentResult
    val forecast = s"${targetScore._1}:${targetScore._2}"

    if (previousSelection != forecast) {
      if (previousSelection != "-:-") {
        logger.info(s"      Updating from '$previousSelection' to '$forecast'.")
        gpMatch.currentBetElem.click()
      }
      gp.highlight(gpMatch.hostsElem)
      gpMatch.hostsElem.click()
      for (_ <- 0 until targetScore._1) {
        gpMatch.hostsElem.click()
        Thread.sleep(500)
      }
      for (_ <- 0 until targetScore._2) {
        gpMatch.guestsElem.click()
        Thread.sleep(500)
      }
      publishLog(s"Selection for match ${gpMatch.host}:${gpMatch.guest}: $forecast" + reason.map(x => s" ($x)").getOrElse(""))
      1
    } else {
      logger.info(s"      Forecast is the same as previous.")
      0
    }
  }

  def oddsToScore(host: Double, guest: Double): (Int, Int) = {
    val less = Math.min(host, guest)
    val score = if (less < 1.2) 3 else if (less < 1.8) 2 else 1
    if (host < guest) (score, 0) else (0, score)
  }
}
