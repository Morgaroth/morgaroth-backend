package io.github.morgaroth.gpbettingleague

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import io.github.morgaroth.base.{MMarshalling, UserCredentials}
import io.github.morgaroth.gpbettingleague.xpath._
import org.joda.time.{DateTime, LocalTime}
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{MediaRanges, MediaType, MediaTypes, headers}
import akka.http.scaladsl.model.headers._
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.{By, WebElement}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Try

object gp extends Selenium with MMarshalling {

  def loginToGPBettingLeague(login: String, password: String)(implicit wd: Driver): Unit = loginToGPBettingLeague(UserCredentials(login, password))

  def loginToGPBettingLeague(creds: UserCredentials)(implicit wd: Driver): Unit = {
    go to "https://bettingleaguegp.appspot.com"
    if (currentUrl == "https://bettingleaguegp.appspot.com/login.jsp") {
      val loginInput = findElement(By.name("login"))
      val passInput = findElement(By.name("password"))
      loginInput.sendKeys(creds.user)
      passInput.sendKeys(creds.password)
      loginInput.submit()
    }
  }

  def getActiveRounds()(implicit driver: Driver): List[String] = {
    import scala.collection.JavaConverters._
    driver.until(
      ExpectedConditions.presenceOfElementLocated(x"//ul[@data-type='side-menu-month']")
    )
    findElements(x"//ul[@data-type='side-menu-month']//li[contains(@class, 'green-highlight')]//a[starts-with(@href, 'round.jsp')]")
      .map(_.getAttribute("href"))
  }

  def getSaveButton()(implicit wd: Driver): WebElement = {
    findElement(x"//button[@type='button' and text()='Save my bets!']")
  }

  def getMatches(link: String)(implicit wd: Driver): List[GpMatch] = {
    go to link
    val (future, _) = findElements(x"//tr[@data-match-id]").partition(_.getAttribute("data-type") == "match")
    future.map { row =>
      val daysToStart = Try(row.findElement(x".//div[@data-type='time-left-text']").getText.stripSuffix(" days").toInt).getOrElse(0)
      val start = row.findElement(x"./td[@id='time']")
      val guests = row.findElement(x"./td[contains(@class, 'bet') and contains(@class, 'left')]")
      val hosts = row.findElement(x"./td[contains(@class, 'bet') and contains(@class, 'right')]")
      val currentResult = row.findElement(x"./td[contains(@class, 'bet') and @data-type='result']")
      val matchId = row.getAttribute("data-match-id").toInt
      val startTime = DateTime.now().plusDays(daysToStart).withTime(new LocalTime(start.getText))
      GpMatch(hosts, guests, startTime, currentResult, matchId)
    }
  }

  def getCompletedRounds()(implicit wd: Driver, system: ActorSystem): List[Int] = {
    implicit val mat: ActorMaterializer = ActorMaterializer()
    import system.dispatcher
    val sessionCookie = wd.manage().getCookieNamed("session")
    val result = Http().singleRequest(Get("https://bettingleaguegp.appspot.com/api/rounds?mode=short").withHeaders(
      Cookie("session" -> sessionCookie.getValue),
    ))
    val parsedResponse: Future[List[Int]] = result
      .flatMap(_.entity.toStrict(10.seconds))
      .map(_.data.decodeString("utf-8"))
      .map { rawJson =>
        try MJson.read[RoundsShortResponse](rawJson)
        catch {
          case t: Throwable =>
            println(rawJson)
            throw t
        }
      }.map(_.data.filter(_.completed).map(_.id))
    Await.result(parsedResponse, 20.seconds)
  }

  def getCurrentStatsOfMatches(roundId: String)(implicit wd: Driver, system: ActorSystem): List[MatchStats] = {
    implicit val mat: ActorMaterializer = ActorMaterializer()
    import system.dispatcher
    val sessionCookie = wd.manage().getCookieNamed("session")
    val result = Http().singleRequest(Get(s"https://bettingleaguegp.appspot.com/api/stats?round=$roundId").withHeaders(
      Cookie("session" -> sessionCookie.getValue),
    ))
    val parsedResponse = result
      .flatMap(_.entity.toStrict(10.seconds))
      .map(_.data.decodeString("utf-8"))
      .map { rawJson =>
        try MJson.read[RoundMatchesStats](rawJson)
        catch {
          case t: Throwable =>
            println(rawJson)
            throw t
        }
      }.map(_.matches)
    Await.result(parsedResponse, 20.seconds)
  }

  def getResultsOfRound(roundId: Int)(implicit wd: Driver): List[GpRoundResult] = {
    go to s"https://bettingleaguegp.appspot.com/results.jsp?id=$roundId"
    var rows: List[List[String]] = null
    do {
      rows = findElements(x"//table[@id='results-table']/tbody//tr[not(@data-show-id)]")
        .map(_ /+ x"./td")
        .map(_.map(_.getText).filter(_.nonEmpty))
        .filter(_.nonEmpty)
    } while (rows.contains(List("Loading...")) || rows.isEmpty)
    rows.flatMap {
      case no :: total :: bonus :: player :: Nil =>
        Some(GpRoundResult(no.toInt, total.toInt, bonus.toInt, player, roundId))
      case another =>
        println(s"error handling round result row of $another")
        None
    }
  }
}