package io.github.morgaroth.internalcron

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import io.github.morgaroth.base.configuration.InMemoryConfig
import io.github.morgaroth.base.{AddEntry, Commands, GetEntries, RunGPBettingLeagueTomorrowPreviousPass}
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class InternalCronTest extends TestKit(ActorSystem("cron-test")) with FlatSpecLike with ImplicitSender with BeforeAndAfterAll with Matchers
  with BeforeAndAfterEach with ScalaFutures {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  var cfg: InMemoryConfig = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    cfg = new InMemoryConfig
  }

  val testjob1 = "Test job"
  val testjob2 = "Another test job"
  val cronCommand1 = """["GetEntries",{}]"""
  val cronCommand2 = """["RunGPBettingLeagueTomorrowPreviousPass",{}]"""

  "A cron actor" should "save cron entry in config when add entry and run immediately" in {
    val p = TestProbe()
    system.eventStream.subscribe(p.ref, classOf[Commands])

    val un = system.actorOf(InternalCron.props(cfg))

    un ! AddEntry(testjob1, CronExamples.everyMinute, cronCommand1)

    within(5.seconds) {
      p.expectMsgType[GetEntries.type]
    }
    cfg.data.size shouldBe 2
  }

  it should "ignore job which was previously fired during startup" in {
    cfg.appendToStringArray(InternalCron.jobsCfgKey, testjob1)
    cfg.appendToStringArray(InternalCron.jobsCfgKey, testjob2)

    cfg.put(InternalCron.jobCfgKey(testjob1), CronEntry(CronExamples.everyMinute, cronCommand1, testjob1, Some(DateTime.now.minusSeconds(1))))
    cfg.put(InternalCron.jobCfgKey(testjob2), CronEntry(CronExamples.everyMinute, cronCommand2, testjob2, Some(DateTime.now.minusSeconds(61))))

    val p = TestProbe()
    system.eventStream.subscribe(p.ref, classOf[Commands])

    cfg.getStringArray(InternalCron.jobsCfgKey).futureValue.size shouldBe 2
    system.actorOf(InternalCron.props(cfg))

    p.expectMsgType[RunGPBettingLeagueTomorrowPreviousPass.type](15.seconds)
    p.expectNoMsg()
  }
}