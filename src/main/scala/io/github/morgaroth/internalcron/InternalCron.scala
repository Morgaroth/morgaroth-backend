package io.github.morgaroth.internalcron

import java.util.NoSuchElementException

import akka.actor.Props
import cron4s._
import cron4s.lib.joda._
import io.github.morgaroth.base.FutureHelpers._
import io.github.morgaroth.base._
import io.github.morgaroth.base.configuration.SimpleConfig
import io.github.morgaroth.internalcron.InternalCron.{Check, jobCfgKey, jobsCfgKey}
import org.joda.time
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object InternalCron extends ServiceManager {
  override def initialize(ctx: MContext) = {
    ctx.system.actorOf(props(ctx.cfg))
  }

  def props(cfg: SimpleConfig) = {
    Props(new InternalCron(cfg))
  }

  val jobsCfgKey = "internal-cron.jobs"

  def jobCfgKey(jobName: String) = s"internal-cron.jobs.$jobName"

  case class Check(job: String)

}

case class CronEntry(defString: String, command: String, name: String, lastRun: Option[DateTime])

class InternalCron(cfg: SimpleConfig) extends MorgarothActor {

  subscribe(classOf[CronCommands])

  cfg.getStringArray(jobsCfgKey).recover {
    case _: NoSuchElementException => log.info("No jobs in crontab."); Nil
  }.onComplete {
    case Success(keys) => keys.foreach { cmd => context.system.scheduler.scheduleOnce(10.seconds, hardSelf, Check(jobCfgKey(cmd))) }
    case Failure(thr) => log.error(thr, "Requesting all jobs from configuration end with exception {}", thr.getMessage)
  }

  def getEntry(key: String) = cfg.get[CronEntry](key)

  def findEntry(jobName: String) = getEntry(jobCfgKey(jobName))

  val startOfTime = DateTime.now.withMillis(0L)

  override def receive = {
    case Check(jobId) =>
      log.debug(s"Checking job $jobId")
      (for {
        job <- getEntry(jobId)
        nextRun = Cron.unsafeParse(job.defString).next(job.lastRun.getOrElse(startOfTime)).get
        _ <- if (nextRun.isBeforeNow) {
          CommandBB.interpret(job.command).map { cmd =>
            publish(cmd)
            publishLog(s"Job ${job.name} started.")
            val updatedValue = job.copy(lastRun = Some(DateTime.now))
            cfg.put(jobId, updatedValue).map(Some(_)).logErrors("Updating config entry for {} end with error. (new value: {})", jobId, updatedValue)
          }.getOrElse {
            log.warning("job {} wasn't parsed correctly.", job)
            future(None)
          }
        } else {
          val delay = (nextRun.getMillis - DateTime.now.getMillis).millis + 2.seconds
          log.info(s"Scheduling job ${job.name} in $delay.")
          context.system.scheduler.scheduleOnce(delay, self, Check(jobId))
          future(None)
        }
      } yield ()).logErrors("fds")
    case e@AddEntry(name, strDef, task) =>
      log.debug("Got {} command", e)
      for {
        _ <- cfg.appendToStringArray(jobsCfgKey, name)
        entry: CronEntry = CronEntry(strDef, task, name, None)
        _ <- cfg.put(jobCfgKey(name), entry).logErrors("Adding crontab entry {} end with error.", e).whenCompleted {
          case Success(_) => publishLog(s"Crontab entry for $name added.")
          case Failure(thr) => publishLog(s"Adding crontab entry for $name failed with error ${thr.getMessage}.")
        }
        _ = hardSelf ! Check(jobCfgKey(name))
        _ = sendToClient("CrontabEntryAdded")
      } yield ()
    case UpdateEntry(name, None, None) =>
      log.warning(s"No updates in update request for crontab entry $name")
    case c@RemoveEntry(name) =>
      log.debug("Got request {}", c)
      for {
        _ <- cfg.removeFromStringArray(jobsCfgKey, name)
        _ <- cfg.remove(jobCfgKey(name))
        _ = sendToClient("CrontabEntryRemoved")
      } yield ()
    case e@UpdateEntry(name, strDef, task) =>
      for {
        prev <- findEntry(name)
        newValue = prev.copy(defString = strDef.getOrElse(prev.defString), command = task.getOrElse(prev.command))
        _ <- cfg.put(jobCfgKey(name), newValue).logErrors("Updating crontab entry {} with updates {} end with error.", prev, e).whenCompleted {
          case Success(_) => publishLog(s"Crontab entry for $name updated.")
          case Failure(thr) => publishLog(s"Updating crontab entry for $name failed with error ${thr.getMessage}.")
        }
      } yield ()
    case GetEntries =>
      cfg.getStringArray(jobsCfgKey).flatMap(entries => future(entries.map(findEntry))).map { entries =>
        sendToClient("CrontabEntries", entries)
      }
  }

  override val logSourceName = "Cron"
}