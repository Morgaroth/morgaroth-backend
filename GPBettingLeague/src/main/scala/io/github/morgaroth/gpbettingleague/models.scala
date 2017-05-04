package io.github.morgaroth.gpbettingleague

import org.joda.time.DateTime
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.{WebDriver, WebElement}

import scala.language.implicitConversions

object Universal {
  // universal name -> ( GP names, OC names )
  val data = Map(
    "Ajax Amsterdam" -> (List("Ajax Amsterdam"), List("Ajax")),
    "AS Roma" -> (List("AS Roma"), List("Roma")),
    "Atletico Madrid" ->(List("Atletico Madrid"), List("Atlético Madrid", "Atletico de Madrid")),
    "Betis" -> (List("Betis"), List("Real Betis")),
    "Bournemouth" -> (List("AFC Bournemouth"), List("Bournemouth")),
    "Borussia Monchengladbach" -> (List("Borussia Moenchengladbach"), List("Borussia Monchengladbach")),
    "Cracovia" -> (List("Cracovia"), List("Cracovia Krakow")),
    "Genk" -> (List("Racing Genk"), List("Genk")),
    "Gijon" -> (List("Sporting Gijon"), List("Gijon")),
    "Hamburg" -> (List("Hamburger SV"), List("Hamburg")),
    "Hull" -> (List("Hull City"), List("Hull")),
    "Inter Milan" -> (List("Inter Milan"), List("Inter")),
    "Kolonia" -> (List("FC Cologne"), List("FC Koln")),
    "Leicester" -> (List("Leicester City"), List("Leicester")),
    "Legia Warszawa" -> (List("Legia Warszawa"), List("Legia Warsaw")),
    "Mainz" -> (List("FSV Mainz 05"), List("Mainz 05")),
    "Man City" -> (List("Manchester City"), List("Man City")),
    "Man Utd" -> (List("Manchester United"), List("Man Utd")),
    "Stoke" -> (List("Stoke City"), List("Stoke")),
    "Real Betis" -> (List("Real Betis"), List("Betis")),
    "Swansea" -> (List("Swansea City"), List("Swansea")),
    "Termalica Nieciecza" -> (List("Termalica Nieciecza"), List("Termalica BB Nieciecza")),
    "West Ham" -> (List("West Ham United"), List("West Ham")),
    "West Brom" -> (List("West Brom"), List("West Bromwich Albion")),
    "Tottenham Hotspur" -> (List("Tottenham Hotspur"), List("Tottenham")),
  )

  val gpToUni = data.flatMap(x => x._2._1.map(_ -> x._1))
  val ocToUni = data.flatMap(x => x._2._2.map(_ -> x._1))

  def gpUid(m: GpMatch) = s"${gpToUni.getOrElse(m.host, m.host)}:${gpToUni.getOrElse(m.guest, m.guest)}"

  def ocUid(m: OCMatch) = s"${ocToUni.getOrElse(m.host, m.host)}:${ocToUni.getOrElse(m.guest, m.guest)}"
}


case class GpMatch(hostsElem: WebElement, guestsElem: WebElement, start: DateTime, currentBetElem: WebElement) {

  def host = hostsElem.getText

  def guest = guestsElem.getText

  lazy val uId = Universal.gpUid(this)

  def currentResult = currentBetElem.getText

  override def toString = s"GPMatch($host:$guest ($currentResult) ${start.toString("dd MMM HH:mm")})"
}

case class OCMatch(host: String, guest: String, hostBet: Double, drawBet: Double, guestBet: Double, start: DateTime) {
  lazy val id = s"$host:$guest"

  lazy val uId = Universal.ocUid(this)

  override def toString = s"OCMatch($host:$guest, ($hostBet,$drawBet,$guestBet) ${start.toString("dd MMM HH:mm")})"

}


case class Driver(wd: WebDriver, wdw: WebDriverWait)

object Driver {
  implicit def fromsingle(implicit wd: WebDriver): Driver = Driver(wd, new WebDriverWait(wd, 10))

  implicit def unwrap(wd: Driver): WebDriver = wd.wd

  implicit def unwrap2(wd: Driver): WebDriverWait = wd.wdw
}