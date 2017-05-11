package io.github.morgaroth.phonefetcher

import akka.NotUsed
import akka.actor.Props
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import better.files._
import io.github.morgaroth.base._

import scala.concurrent.duration._

object PhonePhotosFetcher extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(new PhonePhotosFetcher(ctx)))
  }
}

private class UDevAdmMonitor extends MorgarothActor {

  import scala.sys.process._

  implicit val mat = ActorMaterializer()

  Source("/sbin/udevadm monitor --udev".lineStream_!)
    //    .log("received line from udev")
    .throttle(1, 5.seconds, 1, ThrottleMode.Shaping)
    .to(Sink.actorRef(self, NotUsed))
    .run()

  override def receive = {
    case line: String =>
      //      log.info(s"received from udev $line")
      context.parent ! CheckConnectedDevices()
  }
}

class PhonePhotosFetcher(ctx: ConfigProvider) extends MorgarothActor {
  context.system.eventStream.subscribe(self, classOf[PhotoManagerCommands])

  if (System.getProperty("os.name") == "Linux") {
    context.actorOf(Props(new UDevAdmMonitor), "os-listener")
  } else {
    publishLog("Phone fetcher not initialized because not on linux.")
  }

  override def receive = {
    case CheckConnectedDevices() =>
      val dirs = file"/run/user/" glob "*mtp*"
      if (dirs.nonEmpty) println(dirs.map(_.path).mkString("\n"))
  }

  override def logSourceName = "Photos"
}