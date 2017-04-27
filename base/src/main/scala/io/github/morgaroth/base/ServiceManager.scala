package io.github.morgaroth.base

import akka.actor.ActorSystem

trait ServiceManager {
  def initialize(system: ActorSystem)
}