package io.github.morgaroth.gpbettingleague

import java.io.File
import java.util.Locale
import javax.imageio.ImageIO

import io.github.morgaroth.gpbettingleague.xpath._
import org.joda.time.DateTime
import org.openqa.selenium.{By, JavascriptExecutor, WebElement}
import ru.yandex.qatools.ashot.AShot
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider
import ru.yandex.qatools.ashot.shooting.ShootingStrategies

import scala.language.implicitConversions

/**
  * Created by PRV on 02.04.2017.
  */
trait Selenium {

  object go {
    def to(url: String)(implicit webDriver: Driver): Unit = {
      val parts = url.split("#")
      println(parts.toList)
      webDriver.navigate.to(parts(0))
      if (parts.length == 2) {
        webDriver.wd.asInstanceOf[JavascriptExecutor].executeScript(s"window.location.hash='#${parts(1)}'")
      }
    }
  }

  implicit def convertToby(s: String): By = By.xpath(s)

  //  def currentUrl(implicit wd:WebDriver) = {}
  def currentUrl(implicit wd: Driver): String = {
    wd.getCurrentUrl
  }

  def findElements(by: By)(implicit wb: Driver): List[WebElement] = {
    import scala.collection.JavaConverters._
    wb.findElements(by).asScala.toList
  }

  def findElement(by: By)(implicit wb: Driver): WebElement = {
    import scala.collection.JavaConverters._
    val res = wb.findElements(by).asScala
    if (res.size > 1) {
      throw new IllegalArgumentException(s"to much results ${res.length} for query $by ($res)")
    }
    res.head
  }


  implicit class WrapToXpathable(we: WebElement) {
    def /+(xpath: String): List[WebElement] = {
      import scala.collection.JavaConverters._
      we.findElements(xpath).asScala.toList
    }

    def /(xpath: String): WebElement = {
      import scala.collection.JavaConverters._
      val res = we.findElements(xpath).asScala
      if (res.size > 1) {
        throw new IllegalArgumentException(s"to much results ${res.length} for query $xpath ($res)")
      }
      res.head
    }
  }

  def highlight(e: WebElement)(implicit driver: Driver) {
    import org.openqa.selenium.interactions.Actions
    val action = new Actions(driver)
    action.moveToElement(e).build.perform()
  }

  def takeScreenShot()(implicit driver: Driver): Unit = {
    val elem = findElement(x"//table[@class='table table-striped']")
    ImageIO.write(new AShot().shootingStrategy(ShootingStrategies.viewportPasting(100)).coordsProvider(new WebDriverCoordsProvider()).takeScreenshot(driver, elem).getImage, "PNG", new File(s"screenshots/${DateTime.now().toString("dd MMM yyyy (HH mm)", Locale.ENGLISH)}.png"))
  }

}
