package io.github.morgaroth.gpbettingleague

import org.joda.time.{DateTime, Days, Minutes}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions

import scala.language.implicitConversions

object Main {

  def main(args: Array[String]): Unit = {
    run(args(0))
  }

  def run(password: String) {
    println(s"running with $password")
    System.setProperty("webdriver.chrome.driver", "C:/Users/PRV/projects/MorgarothServer/GPBettingLeague/chromedriver.exe")
    implicit val driver: WebDriver = new ChromeDriver()

    val ocBets = oc.scrapMatches().map(x => x.uId -> x).toMap

    gp.getActiveRounds(password).foreach { round =>
      val data = gp.getMatches(round)

      val changes = data.map { m =>
        println(s"  ${m.host} vs ${m.guest}")
        ocBets.get(m.uId) map { ocMatch =>
          println(s"    Diff in minutes: ${Minutes.minutesBetween(ocMatch.start, m.start).getMinutes}")
          println(s"    Odds:")
          println(s"      ${ocMatch.host}: ${ocMatch.hostBet}")
          println(s"      Draw: ${ocMatch.drawBet}")
          println(s"      ${ocMatch.guest}: ${ocMatch.guestBet}")
          val targetScore = oddsToScore(ocMatch.hostBet, ocMatch.guestBet)
          println(s"      Forecast ${targetScore._1}:${targetScore._2}")
          if (m.currentResult != "-:-") {
            m.currentBetElem.click()
          }
          gp.highlight(m.hostsElem)
          m.hostsElem.click()
          for (i <- 0 until targetScore._1) {
            m.hostsElem.click()
            Thread.sleep(500)
          }
          for (i <- 0 until targetScore._2) {
            m.guestsElem.click()
            Thread.sleep(500)
          }
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
          0
        }
      }.sum
      if (changes > 0) {
        val submit = gp.getSaveButton()
        println("Submit!")
        submit.click()
        Option(ExpectedConditions.alertIsPresent()(driver)).foreach { _ =>
          driver.switchTo().alert().accept()
        }
      } else {
        println("No matches to bet.")
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
