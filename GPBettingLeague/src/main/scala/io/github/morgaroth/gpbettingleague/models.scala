package io.github.morgaroth.gpbettingleague

import org.joda.time.DateTime
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.{WebDriver, WebElement}

import scala.language.implicitConversions

object GPNames {
  def apply(name: String, names: String*) = name :: name.toList
}

object OCNames {
  def apply(name: String, names: String*) = name :: name.toList
}

object Universal {
  // universal name -> ( GP names, OC names )
  val data = Map(
    "Ajax Amsterdam" -> List("Ajax"),
    "AS Roma" -> List("Roma"),
    "Atletico Madrid" -> List("Atlético Madrid", "Atletico de Madrid"),
    "Austria Wien" -> List("Austria Wien", "Austria Vienna"),
    "Bastia" -> List("SC Bastia"),
    "Betis" -> List("Real Betis"),
    "Bournemouth" -> List("AFC Bournemouth"),
    "Cracovia" -> List("Cracovia Krakow"),
    "Genk" -> List("Racing Genk"),
    "Gijon" -> List("Sporting Gijon"),
    "Hamburg" -> List("Hamburger SV"),
    "Hannover 96" -> List("Hannover 0.0"),
    "Hull" -> List("Hull City"),
    "Inter Milan" -> List("Inter"),
    "Kolonia" -> List("FC Cologne", "FC Koln"),
    "Leicester" -> List("Leicester City"),
    "Legia Warszawa" -> List("Legia Warsaw"),
    "Lokomotiv Moscow" -> List("Lok Moscow"),
    "Ludogorets" -> List("Ludogorets Razgrad"),
    "Macedonia" -> List("FYR Macedonia"),
    "Mainz" -> List("FSV Mainz 05", "Mainz 05"),
    "Man City" -> List("Manchester City"),
    "Man Utd" -> List("Manchester United"),
    "Northern Ireland" -> List("North. Ireland"),
    "Olympiacos" -> List("Olympiakos"),
    "Paris St Germain" -> List("Paris Saint Germain", "Paris St Germain", "Paris Saint-Germain"),
    "Real Betis" -> List("Real Betis", "Betis"),
    "Republic of Ireland" -> List("Ireland Rep", "Republic of Ireland"),
    "Saint-Etienne" -> List("St Etienne"),
    "SPAL 2013" -> List("Spal"),
    "Stoke" -> List("Stoke City"),
    "Sporting CP" -> List("Sporting Lisbon"),
    "Swansea" -> List("Swansea City"),
    "Termalica Nieciecza" -> List("Termalica BB Nieciecza", "Termalica Bruk-Bet Nieciecza"),
    "Tottenham Hotspur" -> List("Tottenham"),
    "West Ham" -> List("West Ham United"),
    "West Brom" -> List("West Bromwich Albion"),
    "Vitoria de Guimaraes" -> List("Vitoria Guimaraes"),
    "Zulte-Waregem" -> List("Zulte Waregem"),
  )

  val toUni = data.flatMap(x => x._2.map(_ -> x._1))

  def gpUid(m: GpMatch) = s"${toUni.getOrElse(m.host, m.host)}:${toUni.getOrElse(m.guest, m.guest)}"

  def ocUid(m: OCMatch) = s"${toUni.getOrElse(m.host, m.host)}:${toUni.getOrElse(m.guest, m.guest)}"
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