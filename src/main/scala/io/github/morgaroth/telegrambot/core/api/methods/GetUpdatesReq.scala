package io.github.morgaroth.telegrambot.core.api.methods

import scala.concurrent.duration.FiniteDuration

case class GetUpdatesReq(
                          offset: Option[Int] = None,
                          limit: Option[Int] = None,
                          timeout: Option[Int] = None
                        )

object GetUpdatesReq {

  def apply(offset: Option[Int], limit: Int): GetUpdatesReq =
    apply(offset, Some(limit))

  def apply(offset: Option[Int], timeout: FiniteDuration): GetUpdatesReq =
    apply(offset, None, Some(timeout.toSeconds.toInt))

  def apply(offset: Option[Int], limit: Int, timeout: FiniteDuration): GetUpdatesReq =
    apply(offset, Some(limit), Some(timeout.toSeconds.toInt))

}