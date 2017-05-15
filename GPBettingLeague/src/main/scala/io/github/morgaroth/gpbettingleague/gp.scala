package io.github.morgaroth.gpbettingleague

import io.github.morgaroth.base.UserCredentials
import io.github.morgaroth.gpbettingleague.xpath._
import org.joda.time.{DateTime, LocalTime}
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions

import scala.util.Try

object gp extends Selenium {

  def loginToGPBettingLeague(creds: UserCredentials)(implicit wd: Driver) = {
    go to "http://bettingleaguegp.appspot.com"
    if (currentUrl == "http://bettingleaguegp.appspot.com/login.jsp") {
      val loginInput = findElement(By.name("login"))
      val passInput = findElement(By.name("password"))
      loginInput.sendKeys(creds.user)
      passInput.sendKeys(creds.password)
      loginInput.submit()
    }
  }

  def getActiveRounds()(implicit driver: Driver) = {
    import scala.collection.JavaConverters._
    driver.until(ExpectedConditions.not(ExpectedConditions.textToBePresentInElementLocated(By.id("side-menu-running"), "Loading...")))
    findElement(By.id("side-menu-running"))
      .findElements(x".//a[starts-with(@href, 'round.jsp')]")
      .asScala.toList
      .map(_.getAttribute("href"))
  }

  def getSaveButton()(implicit wd: Driver) = {
    findElement(x"//button[@type='button' and text()='Save my bets!']")
  }

  def getMatches(link: String)(implicit wd: Driver) = {
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
}