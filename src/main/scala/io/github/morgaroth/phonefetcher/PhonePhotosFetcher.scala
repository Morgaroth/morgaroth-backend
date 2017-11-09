package io.github.morgaroth.phonefetcher

import akka.actor.{Cancellable, Props}
import akka.event.Logging
import better.files._
import io.github.morgaroth.Directories
import io.github.morgaroth.base._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.sys.process._

object PhonePhotosFetcher extends ServiceManager {
  override def initialize(ctx: MContext): Unit = {
    if (ctx.staticCfg.getBoolean("photos-manager.enabled")) {
      ctx.system.actorOf(Props(new PhonePhotosFetcher(ctx)))
    } else {
      Logging(ctx.system, getClass).info("PhonePhotos disabled in config file.")
    }
  }
}


class PhonePhotosFetcher(ctx: ConfigProvider) extends MorgarothActor {
  context.system.eventStream.subscribe(self, classOf[PhotoManagerCommands])

  val userId: Int = "id -u".!!.trim.replace("\"", "").toInt

  log.info(s"userId is $userId")

  def checkPhone(): Cancellable = context.system.scheduler.scheduleOnce(2.seconds, self, CheckPhoneConnected())

  def checkEHDrive(): Cancellable = context.system.scheduler.scheduleOnce(2.seconds, self, CheckExternalDriveConnected())


  val tmpStore = better.files.File(ctx.staticCfg.getString("photos-manager.tmp-files-directory"))
  if (tmpStore.isRegularFile) {
    log.error(s"$tmpStore is a file, cannot store there any files")
  } else if (!tmpStore.exists) tmpStore.createDirectories()

  override def receive: Receive = {
    case CheckPhoneConnected() =>
      val dirs = file"/run/user/$userId/gvfs/".list.filter(_.name.startsWith("mtp:")).flatMap(_.list)
      if (dirs.nonEmpty) {
        dirs foreach { dir =>
          val photosSource = dir / "DCIM-2"
          if ((dir / "Server" isRegularFile) && (photosSource isDirectory) && (photosSource nonEmpty)) {
            log.info(s"${dir.path} will be used to fetch photos")
            Directories.merge(photosSource, tmpStore)
          }
        }
      }
      checkPhone()
    case CheckExternalDriveConnected() =>
    //      val dirs = file"/run/user/$userId/gvfs/".list.filter(_.name.startsWith("mtp:")).flatMap(_.list)
    //      if (dirs.nonEmpty) {
    //        dirs foreach { dir =>
    //          if ((dir / "Server" isRegularFile) && (dir / "DCIM" isDirectory) && (dir / "DCIM" nonEmpty)) {
    //            log.info(s"${dir.path} will be used to fetch photos")
    //            Directories.merge(tmpStore,_)
    //          }
    //        }
    //      }
    //      checkEHDrive()
    case msg =>
      log.warning(s"unhandled $msg")
  }

  checkPhone()
  log.info("Initialized.")

  override def logSourceName = "Photos"
}