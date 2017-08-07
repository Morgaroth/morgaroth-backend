package io.github.morgaroth.gpbettingleague

import org.scalatest.{FlatSpec, Matchers}

class DateParserTest extends FlatSpec with Matchers {

  "string's replaceAll" should "replace endings of numbers correctly" in {
    val normalized = oc.normalizeDateString("Monday 7th August 2017")
    normalized shouldBe "Monday 7 August 2017"
  }
}
