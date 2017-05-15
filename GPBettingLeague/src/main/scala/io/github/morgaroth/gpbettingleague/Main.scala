package io.github.morgaroth.gpbettingleague

import java.net.URL

import akka.actor.ActorSystem
import akka.event.Logging
import com.typesafe.config.Config
import io.github.morgaroth.base.{MessagesPublisher, UserCredentials}
import org.joda.time.{DateTime, Days, Minutes}
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.support.ui.ExpectedConditions

import scala.language.implicitConversions

class Main(cfg: Config)(implicit as: ActorSystem) extends MessagesPublisher {
  implicit val log = Logging(as, getClass)

  override def logSourceName: String = "GP Betting Runner"

  implicit lazy val driver = if (cfg.hasPath("remote-server")) {
    log.info(s"running with remote server on ${cfg.getString("remote-server")}")
    new RemoteWebDriver(new URL(cfg.getString("remote-server")), DesiredCapabilities.chrome)
  } else {
    log.info(s"running with local driver ${cfg.getString("driver-path")}")
    System.setProperty("webdriver.chrome.driver", cfg.getString("driver-path"))
    new ChromeDriver()
  }

  def shutdown() {
    driver.quit()
  }

  def run(creds: UserCredentials, newerThan: Option[DateTime]) {
    val ocBets = oc.scrapMatches(newerThan).map(x => x.uId -> x).toMap

    gp.loginToGPBettingLeague(creds)
    log.debug("Logged into GPBettingLeague page")

    gp.getActiveRounds().foreach { round =>
      val data = gp.getMatches(round).filter(x => newerThan.forall(_.isAfter(x.start)))

      val changes = data.map { m =>
        println(s"  ${m.host} vs ${m.guest}")
        ocBets.get(m.uId) map { ocMatch =>
          println(s"    Diff in minutes: ${Minutes.minutesBetween(ocMatch.start, m.start).getMinutes}")
          println(s"    Odds:")
          println(s"      ${ocMatch.host}: ${ocMatch.hostBet}")
          println(s"      Draw: ${ocMatch.drawBet}")
          println(s"      ${ocMatch.guest}: ${ocMatch.guestBet}")
          val targetScore = oddsToScore(ocMatch.hostBet, ocMatch.guestBet)
          val score = s"${targetScore._1}:${targetScore._2}"
          println(s"      Forecast $score")
          if (m.currentResult != "-:-") {
            m.currentBetElem.click()
          }
          gp.highlight(m.hostsElem)
          m.hostsElem.click()
          for (_ <- 0 until targetScore._1) {
            m.hostsElem.click()
            Thread.sleep(500)
          }
          for (_ <- 0 until targetScore._2) {
            m.guestsElem.click()
            Thread.sleep(500)
          }
          publishLog(s"Selection for match ${m.host}:${m.guest}: $score")
          1
        } getOrElse {
          val possible = ocBets.filter(b => b._1.contains(m.guest.take(5)) || b._1.contains(m.host.take(5))).toList
          println(s"\nNo match for ${m.host} vs ${m.guest} at (${m.start.toString("dd MMM HH:mm")}).")
          val daysToStart = Days.daysBetween(DateTime.now(), m.start).getDays
          if (daysToStart > 6) {
            println(s"\tBut it is match in far future, $daysToStart days from now.")
          }
          println(s"\tBut there are ${possible.length} options:")
          println(s"\t\t${possible.mkString("\n\t\t")}")
          publishLog(s"No match ${m.host} vs ${m.guest} at (${m.start.toString("dd MMM HH:mm")}).")
          0
        }
      }.sum
      if (changes > 0) {
        val submit = gp.getSaveButton()
        publishLog(s"Selections for round #${round.stripPrefix("http://bettingleaguegp.appspot.com/round.jsp?id=")} made.")
        submit.click()
        Option(ExpectedConditions.alertIsPresent()(driver)).foreach { _ =>
          driver.switchTo().alert().accept()
        }
      } else {
        publishLog(s"No matches to make selections for round #${round.stripPrefix("http://bettingleaguegp.appspot.com/round.jsp?id=")}.")
      }
    }
    driver.close()
  }

  def oddsToScore(host: Double, guest: Double): (Int, Int) = {
    val less = Math.min(host, guest)
    val score = if (less < 1.2) 3 else if (less < 1.8) 2 else 1
    if (host < guest) (score, 0) else (0, score)
  }
}
