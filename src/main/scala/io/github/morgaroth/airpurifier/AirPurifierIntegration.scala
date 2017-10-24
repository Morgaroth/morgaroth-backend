package io.github.morgaroth.airpurifier

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.github.morgaroth.airpurifier.AirPurifierIntegration.RefreshPurifiersList
import io.github.morgaroth.base.FutureHelpers._
import io.github.morgaroth.base._

import scala.concurrent.Future
import scala.concurrent.duration._

object AirPurifierIntegration extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(new AirPurifierIntegration(
      miioServiceUrl = ctx.staticCfg.getString("miio.serviceUrl")
    )))
  }

  case object RefreshPurifiersList

}

class AirPurifierIntegration(miioServiceUrl: String) extends Actor with ActorLogging with FailFastCirceSupport {
  context.system.eventStream.subscribe(self, classOf[AirPurifierCommands])

  import context.{dispatcher, system}

  implicit val mat = ActorMaterializer.create(context.system)

  var currentAirPurifiers = List.empty[Device]

  self ! RefreshPurifiersList

  log.info(s"service api is $miioServiceUrl")

  override def receive = {
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

    case EventLog(source, msg, _) =>
      log.info(s"Event: $source - $msg")

    case unhandled =>
      log.error("Unhandled message {} of class {}", unhandled, unhandled.getClass.getCanonicalName)
  }

  def power(state: String, dev: Device) = {
    req(Post(s"$miioServiceUrl/devices/${dev.ip}", StrTask("power", state)))
  }

  def powerOn(dev: Device) = power("on", dev)

  def powerOff(dev: Device) = power("off", dev)

  def checkAvailableDevices(): Future[DevicesList] = {
    for {
      _ <- req(Patch(s"$miioServiceUrl/refresh"))
      _ <- after(5.seconds, context.system.scheduler)(succ(1))
      devicesResp <- req(Get(s"$miioServiceUrl/devices"))
      devices <- Unmarshal(devicesResp).to[List[Device]]
      _ = log.info(s"refreshed $devices")
    } yield DevicesList(devices)
  }

  def req(url: HttpRequest) = {
    Http().singleRequest(url).map { resp =>
      log.info(s"received response from ${url.uri}: $resp")
      resp
    }
  }
}