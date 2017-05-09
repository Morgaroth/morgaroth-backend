package io.github.morgaroth.base

import akka.actor.ActorSystem
import io.github.morgaroth.base.configuration.SimpleConfig

/**
  * Created by PRV on 08.05.2017.
  */
trait BaseActorSystem {
  def system: ActorSystem
}

trait ConfigProvider {
  def configProvider: SimpleConfig
}

trait MContext extends BaseActorSystem with ConfigProvider