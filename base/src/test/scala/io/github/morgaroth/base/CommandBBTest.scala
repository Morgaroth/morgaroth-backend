package io.github.morgaroth.base

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by PRV on 11.05.2017.
  */
class CommandBBTest extends FlatSpec with Matchers {

  "CommandBB extractor" should "parse gpbetting league command correctly" in {
    val text = """["RunGPBettingLeague",{"password":"testpass"}]"""
    CommandBB.interpret(text) shouldBe 'defined
  }
}
