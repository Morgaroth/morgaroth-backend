package io.github.morgaroth.airpurifier

import akka.actor.Props
import akka.actor.Status.Failure
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import akka.stream.{ActorMaterializer, StreamTcpException}
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.github.morgaroth.airpurifier.AirPurifierIntegration.RefreshPurifiersList
import io.github.morgaroth.base._

import scala.concurrent.Future
import scala.concurrent.duration._

object AirPurifierIntegration extends ServiceManager with LazyLogging {
  override def initialize(ctx: MContext): Unit = {
    if (ctx.staticCfg.getBoolean("air-purifier.enabled")) {
      ctx.system.actorOf(Props(new AirPurifierIntegration(
        miioServiceUrl = ctx.staticCfg.getString("air-purifier.miio.serviceUrl")
      )))
    } else logger.info("AirPurifierIntegration disabled in config file.")
  }

  case object RefreshPurifiersList

}

class AirPurifierIntegration(miioServiceUrl: String) extends MorgarothActor with FailFastCirceSupport {
  context.system.eventStream.subscribe(self, classOf[AirPurifierCommands])

  implicit val mat: ActorMaterializer = ActorMaterializer.create(context.system)

  var currentAirPurifiers = List.empty[Device]

  context.system.scheduler.schedule(2.seconds, 5.seconds, self, RefreshPurifiersList)

  log.info(s"service api is $miioServiceUrl")

  override def receive: Receive = {
    case RefreshPurifiersList =>
      pipe(checkAvailableDevices()).pipeTo(self)
    case DevicesList(devices) =>
      currentAirPurifiers = devices.filter(_.kind == "AirPurifier")
    case PowerOn =>
      currentAirPurifiers.foreach(powerOn)
    case PowerOff =>
      currentAirPurifiers.foreach(powerOff)
    case SetLedBrightness(level) if Set("bright", "dim", "off").contains(level.toLowerCase()) =>

    case SetOperateMode(mode) if Set("auto", "silent", "favorite", "idle").contains(mode.toLowerCase()) =>

    case SetFavoriteLevel(level) if level >= 0 && level <= 17 =>

    case AirPurifierStatus =>
      currentAirPurifiers.foreach { dev =>
        val resp =
          s"""
             |Power: ${dev.status.power}
             |Speed: ${dev.status.motor1_speed}
             |PM2: ${dev.status.aqi}µg/m3
             |Wilgotność: ${dev.status.humidity}%
             |Temperatura: ${dev.status.temp_dec / 10.0}°C
        """.stripMargin
        publishLog(resp)
      }

    case Failure(t: StreamTcpException) if t.getMessage.contains("Połączenie odrzucone") =>
    //      log.info("http-miio is missing in the network")

    case unhandled =>
      log.error("Unhandled message {} of class {}", unhandled, unhandled.getClass.getCanonicalName)
  }

  private def power(state: String, dev: Device) = {
    req(Post(s"$miioServiceUrl/devices/${dev.ip}", StrTask("power", state))).andThen {
      case _ => hardSelf ! RefreshPurifiersList
    }
  }

  private def powerOn(dev: Device) = power("on", dev)

  private def powerOff(dev: Device) = power("off", dev)

  def checkAvailableDevices(): Future[DevicesList] = {
    for {
      devicesResp <- req(Get(s"$miioServiceUrl/devices"))
      devices <- Unmarshal(devicesResp).to[List[Device]]
      //      _ = log.info(s"refreshed $devices")
    } yield DevicesList(devices)
  }

  private def req(url: HttpRequest) = {
    Http().singleRequest(url).map { resp =>
      //      log.info(s"received response from ${url.uri}: $resp")
      resp
    }
  }

  override val logSourceName = "AirPurifier"
}