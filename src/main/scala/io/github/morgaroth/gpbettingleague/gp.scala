package io.github.morgaroth.gpbettingleague

import io.github.morgaroth.base.UserCredentials
import io.github.morgaroth.gpbettingleague.xpath._
import org.joda.time.{DateTime, LocalTime}
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.{By, WebElement}

import scala.util.Try

object gp extends Selenium {

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
    driver.until(ExpectedConditions.not(ExpectedConditions.textToBePresentInElementLocated(By.id("side-menu-running"), "Loading...")))
    findElement(By.id("side-menu-running"))
      .findElements(x".//a[starts-with(@href, 'round.jsp')]")
      .asScala.toList
      .map(_.getAttribute("href"))
  }

  def getSaveButton()(implicit wd: Driver): WebElement = {
    findElement(x"//button[@type='button' and text()='Save my bets!']")
  }

  def getMatches(link: String)(implicit wd: Driver): List[GpMatch] = {
    go to link
    val (future, past) = findElements(x"//tr[@data-match-id]").partition(_.getAttribute("data-type") == "match")
    future.map { row =>
      val daysToStart = Try(row.findElement(x".//div[@data-type='time-left-text']").getText.stripSuffix(" days").toInt).getOrElse(0)
      val start = row.findElement(x"./td[@id='time']")
      val guests = row.findElement(x"./td[contains(@class, 'bet') and contains(@class, 'left')]")
      val hosts = row.findElement(x"./td[contains(@class, 'bet') and contains(@class, 'right')]")
      val currentResult = row.findElement(x"./td[contains(@class, 'bet') and @data-type='result']")
      val startTime = DateTime.now().plusDays(daysToStart).withTime(new LocalTime(start.getText))
      GpMatch(hosts, guests, startTime, currentResult)
    }
  }

  def getCompletedRounds()(implicit wd: Driver): List[Int] = {
    var elements: List[String] = null
    do {
      elements = findElements(x"//ul[@id='side-menu-completed']//a").map(_.getText).filter(_.nonEmpty).map(_.stripPrefix("#"))
      println(s"elements are $elements")
    } while (elements.contains("Loading...") || elements.isEmpty)
    println(s"raw scrapped $elements")
    elements.map(_.toInt)
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