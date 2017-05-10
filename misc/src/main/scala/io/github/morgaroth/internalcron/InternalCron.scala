package io.github.morgaroth.internalcron

import java.util.NoSuchElementException

import akka.actor.Props
import cron4s._
import cron4s.lib.joda._
import io.github.morgaroth.base.FutureHelpers._
import io.github.morgaroth.base._
import io.github.morgaroth.internalcron.InternalCron.{Check, jobCfgKey, jobsCfgKey}
import org.joda.time.DateTime

import scala.util.{Failure, Success}

object InternalCron extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(Props(new InternalCron(ctx)))
  }

  val jobsCfgKey = "internal-cron.jobs"

  def jobCfgKey(jobName: String) = s"internal-cron.jobs.$jobName"

  case class Check(job: String)

}

case class CronEntry(defString: String, command: String, name: String, lastRun: Option[DateTime])

class InternalCron(ctx: ConfigProvider) extends MorgarothActor {

  subscribe(classOf[CronCommands])

  val futureSelf = self
  ctx.cfg.getStringArray(jobsCfgKey).recover {
    case _: NoSuchElementException => log.info("No jobs in crontab."); Nil
  }.onComplete {
    case Success(keys) => keys.foreach { cmd => futureSelf ! Check(cmd) }
    case Failure(thr) => log.error(thr, "Requesting all jobs from configuration end with exception {}", thr.getMessage)
  }

  def getEntry(key: String) = ctx.cfg.get[CronEntry](key)

  def findEntry(jobName: String) = getEntry(jobCfgKey(jobName))

  val startOfTime = DateTime.now.withMillis(0L)

  override def receive = {
    case Check(jobId) => for {
      job <- getEntry(jobId)
      nextRun = Cron.unsafeParse(job.defString).next(job.lastRun.getOrElse(startOfTime)).get
      _ <- if (nextRun.isBeforeNow) {
        CommandBB.interpret(job.command).map { cmd =>
          publish(cmd)
          publishLog(s"Job ${job.name} started.")
          val updatedValue = job.copy(lastRun = Some(DateTime.now))
          ctx.cfg.put(jobId, updatedValue).map(Some(_)).logErrors("Updating config entry for {} end with error. (new value: {})", jobId, updatedValue)
        }.getOrElse {
          log.warning("job {} wasn't parsed correctly.", job)
          future(None)
        }
      } else future(None)
    } yield ()
    case e@AddEntry(name, strDef, task) => for {
      _ <- ctx.cfg.appendToStringArray(jobsCfgKey, name)
      _ <- ctx.cfg.put(jobCfgKey(name), CronEntry(strDef, task, name, None)).logErrors("Adding crontab entry {} end with error.", e).onComplete {
        case Success(_) => publishLog(s"Crontab entry for $name added.")
        case Failure(thr) => publishLog(s"Adding crontab entry for $name failed with error ${thr.getMessage}.")
      }
    } yield ()
    case UpdateEntry(name, None, None) =>
      log.warning(s"No updates in update request for crontab entry $name")
    case e@UpdateEntry(name, strDef, task) =>
      for {
        prev <- findEntry(name)
        newValue = prev.copy(defString = strDef.getOrElse(prev.defString), command = task.getOrElse(prev.command))
        _ <- ctx.cfg.put(jobCfgKey(name), newValue).logErrors("Updating crontab entry {} with updates {} end with error.", prev, e).logOut {
          case Success(_) => publishLog(s"Crontab entry for $name updated.")
          case Failure(thr) => publishLog(s"Updating crontab entry for $name failed with error ${thr.getMessage}.")
        }
      } yield ()
    case GetEntries =>
      ctx.cfg.getStringArray(jobsCfgKey).map { entries =>
        publish(SSData("CrontabEntries", entries))
      }
  }
}