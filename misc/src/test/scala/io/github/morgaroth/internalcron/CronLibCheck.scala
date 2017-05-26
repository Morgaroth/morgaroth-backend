package io.github.morgaroth.internalcron

import cron4s.Cron
import cron4s.lib.joda._
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by mateusz on 5/15/17.
  */
class CronLibCheck extends FlatSpec with Matchers {

  "Cron library" should "parse every minute * * * * *" in {
    val parse = Cron.parse("0 * * * * ?")
    parse shouldBe 'right
    val a = parse.right.get.next(DateTime.now.minusMonths(5))
    val b = parse.right.get.next(a.get)
    val c = parse.right.get.next(b.get)
    println(a, b, c)
  }

  it should "parse every hour unsing shorter syntax" in {
    val parse = Cron.parse("0 0 * * * ?")
    parse shouldBe 'right
    val a = parse.right.get.next(DateTime.now.minusMonths(5))
    val b = parse.right.get.next(a.get)
    val c = parse.right.get.next(b.get)
    println(a, b, c)
  }
}
