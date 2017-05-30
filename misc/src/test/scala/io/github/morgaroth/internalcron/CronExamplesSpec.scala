package io.github.morgaroth.internalcron

import cron4s.Cron
import cron4s.lib.joda._
import org.joda.time.{DateTime, Minutes, Seconds}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by mateusz on 5/15/17.
  */
class CronExamplesSpec extends FlatSpec with Matchers {

  "Cron examples" should "provide each hour example" in {
    val parse = Cron.parse(CronExamples.everyHour)
    parse shouldBe 'right
    val cron = parse.right.get
    val ts1 = cron.next(DateTime.now).get
    val ts2 = cron.next(ts1).get
    Minutes.minutesBetween(ts1, ts2).getMinutes shouldBe 60
    println(ts1, ts2)
  }

  it should "provide each minute example" in {
    val parse = Cron.parse(CronExamples.everyMinute)
    parse shouldBe 'right
    val cron = parse.right.get
    val ts1 = cron.next(DateTime.now).get
    val ts2 = cron.next(ts1).get
    Seconds.secondsBetween(ts1, ts2).getSeconds shouldBe 60
    println(ts1, ts2)
  }
}
