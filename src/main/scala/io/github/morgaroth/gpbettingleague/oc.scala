package io.github.morgaroth.gpbettingleague

import java.util.Locale

import io.github.morgaroth.gpbettingleague.xpath._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, LocalTime}
import org.openqa.selenium.{By, Cookie}

import scala.language.{postfixOps, reflectiveCalls}
import scala.reflect.internal.Trees
import scala.util.Try

/**
  * Created by PRV on 02.04.2017.
  */
object oc extends Selenium {

  case class SeleniumError(doc: String, problem: Throwable) extends Exception

  private trait Row

  private case class M(host: String, guest: String, odds: (Double, Double, Double), hour: String) extends Row

  private case class D(date: DateTime) extends Row

  val dateParser = DateTimeFormat.forPattern("EEE d MMM y").withLocale(Locale.ENGLISH)

  def normalizeDateString(str: String) =
    str.replaceAll("(\\d+)(st|th|rd|nd) ", "$1 ")

  def parseDateTime(dateStr: String) =
    DateTime.parse(normalizeDateString(dateStr), dateParser).withZoneRetainFields(DateTimeZone.UTC)

  def performScrapFor(link: String, newerThan: Option[DateTime])(implicit driver: Driver): List[OCMatch] = {
    go to link
    Try(findElement(x"//div[@id='promo-modal']//span[@class='inside-close-button']")).foreach { popup =>
      println("Closing popup")
      popup.click()
    }
    try {
      findElements(x"//tr[@class='match-on ' or @class='date first']").view.map { row =>
        row.getAttribute("class") match {
          case "date first" =>
            D(parseDateTime(row.findElement(By.cssSelector("td > p")).getText))
          case "match-on " =>
            highlight(row)
            // AB tests...
            val hour = row / x"./td[@class='time']/div/span"
            val (teams, bets) = try {
              // A:
              val teams1 = row /+ x".//span[@class='fixtures-bet-name']" map {
                _.getText
              } filter (_ != "Draw")
              val bets1 = row /+ x"./td[@data-best-odds]" map {
                _.getAttribute("data-best-odds")
              }
              println("A version")
              teams1(1)
              bets1(2)
              (teams1, bets1)
            } catch {
              case _: Throwable =>
                // B:
                val teams2 = row /+ x".//p[contains(@class, 'fixtures-bet-name')]" map {
                  _.getText
                }
                val bets2 = row /+ x"./td[@data-best-odds]" map { cell => cell.getAttribute("data-best-odds") }
                println("B version")
                (teams2, bets2)
            }
            if (bets.lengthCompare(3) != 0 || teams.lengthCompare(2) != 0) {
              println(s"WTF invalid amount of bets $bets or teams $teams | ${row.getText}")
              null
            } else {
              M(teams.head, teams(1), (bets.head.toDouble, bets(1).toDouble, bets(2).toDouble), hour.getText)
            }
        }
      }.takeWhile {
        case D(d) if newerThan.isDefined => newerThan.exists(_.isAfter(d))
        case _ => true
      }.filter(_ != null).foldLeft((null, Nil): (DateTime, List[OCMatch])) {
        case ((null, _), m: M) => throw new IllegalArgumentException(s"invalid data $m")
        case ((_, acc), D(d)) => (d, acc)
        case ((d, acc), next: M) =>
          val start = d.withTime(new LocalTime(next.hour))
          (d, acc :+ OCMatch(next.host, next.guest, next.odds._1, next.odds._2, next.odds._3, start))
      }._2
    } catch {
      case t: NoSuchElementException =>
        throw SeleniumError(driver.getPageSource, t)
    }
  }

  def scrapMatches(newerThan: Option[DateTime])(implicit driver: Driver) = {
    go to "https://www.oddschecker.com/"
    driver.manage().addCookie(new Cookie.Builder("hideCountryBanner", "true").domain(".oddschecker.com").build())
    driver.manage().addCookie(new Cookie.Builder("cookiePolicy", "true").domain(".oddschecker.com").build())
    List(
      performScrapFor("https://www.oddschecker.com/football/english/premier-league#winner", newerThan),
      performScrapFor("https://www.oddschecker.com/football/champions-league#winner", newerThan),
      performScrapFor("https://www.oddschecker.com/football/italy/serie-a#winner", newerThan),
      performScrapFor("https://www.oddschecker.com/football/italy/serie-b#winner", newerThan),
      performScrapFor("https://www.oddschecker.com/football/italy/coppa-italia#winner", newerThan),
      performScrapFor("https://www.oddschecker.com/football/france/ligue-1#winner", newerThan),
      performScrapFor("https://www.oddschecker.com/football/spain/la-liga-primera", newerThan),
      performScrapFor("https://www.oddschecker.com/football/english/premier-league", newerThan),
      performScrapFor("https://www.oddschecker.com/football/germany/bundesliga", newerThan),
      performScrapFor("https://www.oddschecker.com/football/poland/ekstraklasa", newerThan),
      //      performScrapFor("", newerThan),
    ).flatten
  }

}
