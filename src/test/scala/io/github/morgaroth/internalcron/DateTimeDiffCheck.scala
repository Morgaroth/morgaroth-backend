package io.github.morgaroth.internalcron

import org.joda.time.{DateTime, Minutes}
import org.scalatest.FlatSpec

/**
  * Created by PRV on 11.05.2017.
  */
class DateTimeDiffCheck extends FlatSpec {

  "Diff in minutes between now and date in the future" should "be greater than zero" in {
    val now = DateTime.now
    val future = DateTime.now.plusDays(1)
    assert(Minutes.minutesBetween(now, future).getMinutes > 0)
  }
}
