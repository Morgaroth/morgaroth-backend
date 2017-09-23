package io.github.morgaroth.gpbettingleague


import java.util.Locale

import io.github.morgaroth.gpbettingleague.xpath._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, LocalTime}
import org.openqa.selenium.{By, Cookie}

import scala.language.{postfixOps, reflectiveCalls}
import scala.util.Try

/**
  * Created by PRV on 02.04.2017.
  */
object oc extends Selenium {


  private trait Row

  private case class M(host: String, guest: String, odds: (Double, Double, Double), hour: String) extends Row

  private case class D(date: DateTime) extends Row

  val dateParser = DateTimeFormat.forPattern("EEE d MMM y").withLocale(Locale.ENGLISH)

  def normalizeDateString(str: String) =
    str.replaceAll("(\\d+)(st|th|rd|nd) ", "$1 ")

  def parseDateTime(dateStr: String) =
    DateTime.parse(normalizeDateString(dateStr), dateParser).withZoneRetainFields(DateTimeZone.UTC)

  private def performScrapFor(link: String, newerThan: Option[DateTime])(implicit driver: Driver): List[OCMatch] = {
    go to link
    Try(findElement(x"//div[@id='promo-modal']//span[@class='inside-close-button']")).foreach { popup =>
      println("Closing popup")
      popup.click()
    }
    findElements(x"//tr[@class='match-on ' or @class='date first']").view.map { row =>
      row.getAttribute("class") match {
        case "date first" =>
          D(parseDateTime(row.findElement(By.cssSelector("td > p")).getText))
        case "match-on " =>
          highlight(row)
          val hour = row / x"./td[@class='time']//p"
          val bets = row /+ x"./td[@data-best-odds]" map { cell =>
            (
              (cell / x".//span[@class='add-to-bet-basket']").getAttribute("data-name"),
              cell.getAttribute("data-best-odds")
            )
          }
          if (bets.size != 3) {
            println(s"WTF invalid amount of bets $bets ${row.getText}")
            null
          } else {
            val (h :: d :: g :: Nil) = bets
            M(h._1, g._1, (h._2.toDouble, d._2.toDouble, g._2.toDouble), hour.getText)
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
  }

  def scrapMatches(newerThan: Option[DateTime])(implicit driver: Driver) = {
    go to "https://www.oddschecker.com/"
    driver.manage().addCookie(new Cookie.Builder("hideCountryBanner", "true").domain(".oddschecker.com").build())
    driver.manage().addCookie(new Cookie.Builder("cookiePolicy", "true").domain(".oddschecker.com").build())
    List(
      performScrapFor("https://www.oddschecker.com/football/elite-coupon", newerThan),
      performScrapFor("https://www.oddschecker.com/football/other/poland/ekstraklasa", newerThan),
      performScrapFor("https://www.oddschecker.com/football/world-cup", newerThan)
    ).flatten
  }

}
