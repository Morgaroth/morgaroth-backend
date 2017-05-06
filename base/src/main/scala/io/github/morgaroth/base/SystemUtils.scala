package io.github.morgaroth.base

import java.io.IOException

import akka.event.LoggingAdapter

object SystemUtils {
  def commandInstalled(cmd: String)(implicit log: LoggingAdapter) = {
    import scala.sys.process._
    try {
      cmd.!
      true
    } catch {
      case e: IOException if e.getMessage == s"""Cannot run program "$cmd": error=2, No such file or directory""" =>
        false
      case e: Throwable =>
        log.warning(s"Another error during checking presence of command {}: {} of class {}", cmd, e.getMessage, e.getClass.getCanonicalName)
        true
    }
  }

}
