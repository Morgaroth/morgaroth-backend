package io.github.morgaroth.gpbettingleague

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.{WebDriver, WebElement}

import scala.io.Source
import scala.language.implicitConversions
import scala.util.Try

object Universal extends LazyLogging {
  // universal name -> ( GP names, OC names )

  private val originNamesMapping = Try {
    Source.fromResource("gp_names.csv").getLines().toList.map { line =>
      line.split(";").toList.filter(_.nonEmpty) match {
        case "T" :: gpid :: gpName :: otherNames :: Nil =>
          otherNames.split(",").toList.map(_ -> gpName).toMap
        case "T" :: _ :: _ :: Nil => // no mapping here
          Map.empty
        case other =>
          logger.warn(s"bad format of line $other")
          Map.empty
      }
    }.foldLeft(Map.empty[String, String]) {
      case (acc, elem) => acc ++ elem
    }
  }.recover {
    case thr: Throwable =>
      logger.error(s"error during loading original mapping file $thr")
      Map.empty
  }.get

  // GP name -> other names
  private val data = Map(
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
    "Hannover 96" -> List("Hannover 0.0", "Hannover"),
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
    "Newcastle United" -> List("Newcastle"),
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
    "Termalica Nieciecza" -> List("Termalica BB Nieciecza", "Termalica Bruk-Bet Nieciecza", "Termalica Bruk Bet Nieciecza"),
    "Tottenham Hotspur" -> List("Tottenham"),
    "West Ham" -> List("West Ham United"),
    "West Brom" -> List("West Bromwich Albion"),
    "Vitoria de Guimaraes" -> List("Vitoria Guimaraes"),
    "Verona" -> List("Hellas Verona"),
    "Zulte-Waregem" -> List("Zulte Waregem"),
  )

  private val toUni = data.flatMap(x => x._2.map(_ -> x._1)) ++ originNamesMapping

  def gpUid(m: GpMatch) = s"${toUni.getOrElse(m.host, m.host)}:${toUni.getOrElse(m.guest, m.guest)}"

  def ocUid(m: OCMatch) = s"${toUni.getOrElse(m.host, m.host)}:${toUni.getOrElse(m.guest, m.guest)}"
}


case class GpMatch(hostsElem: WebElement, guestsElem: WebElement, start: DateTime, currentBetElem: WebElement, matchId: Int) {

  def host: String = hostsElem.getText

  def guest: String = guestsElem.getText

  lazy val uId: String = Universal.gpUid(this)

  def currentResult: String = currentBetElem.getText

  override def toString = s"GPMatch($host:$guest ($currentResult) ${start.toString("dd MMM HH:mm")})"
}

case class OCMatch(host: String, guest: String, hostBet: Double, drawBet: Double, guestBet: Double, start: DateTime) {
  lazy val id = s"$host:$guest"

  lazy val uId: String = Universal.ocUid(this)

  override def toString = s"OCMatch($host:$guest, ($hostBet,$drawBet,$guestBet) ${start.toString("dd MMM HH:mm")})"

}

case class GpRoundResult(place: Int, points: Int, bonus: Int, player: String, roundId: Int) {
  def userResult: String = if (place < Int.MaxValue) place.toString else s"not participated"
}

object GpRoundResult {
  def empty(user: String, roundId: Int) = new GpRoundResult(Int.MaxValue, 0, 0, user, roundId)
}

case class Driver(wd: WebDriver, wdw: WebDriverWait)

object Driver {
  implicit def fromsingle(implicit wd: WebDriver): Driver = Driver(wd, new WebDriverWait(wd, 10))

  implicit def unwrap(wd: Driver): WebDriver = wd.wd

  implicit def unwrap2(wd: Driver): WebDriverWait = wd.wdw
}

case class RoundInfo(id: Int, month: String, completed: Boolean)

case class RoundsShortResponse(data: List[RoundInfo])

case class MatchBet(score: String, count: Int) {
  def forecast: (Int, Int) = {
    val (h :: g :: Nil) = score.split(":").toList.map(_.toInt).take(2)
    (h, g)
  }
}

case class MatchStats(matchId: Int, bets: List[MatchBet]) {
  def mostPopularBet: Option[MatchBet] = {
    if (bets.isEmpty) None else bets.groupBy(x => x.count).maxBy(_._1)._2.headOption
  }
}

case class RoundMatchesStats(id: Int, matches: List[MatchStats])