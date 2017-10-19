package io.github.morgaroth.app

import akka.actor.ActorSystem
import akka.event.Logging
import com.typesafe.config.Config
import io.github.morgaroth.base.MContext
import io.github.morgaroth.mongo.MongoConfigProvider

class AppContext(val system: ActorSystem, config: Config) extends MContext {
  val cfg = new MongoConfigProvider(config.getString("mongo.uri"))

  def getLogger(name: String) = Logging(system, name)

  def getLogger(caller: Any) = Logging(system, caller.getClass.getCanonicalName.stripSuffix("$"))

  def staticCfg = config
}