package io.github.morgaroth.gpbettingleague

import io.github.morgaroth.base.configuration.SimpleConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class BettingDB(rawDb: SimpleConfig) {
  def getRound(roundId: Int): Future[GpRoundResult] =
    rawDb.get[GpRoundResult](s"GpBettingRoundResult-$roundId")

  def saveRound(result: GpRoundResult): Future[GpRoundResult] =
    rawDb.put(s"GpBettingRoundResult-${result.roundId}", result)

  def getRoundsKnownList: Future[Set[Int]] =
    rawDb.getIntArray("GPCompletedRounds").recover {
      case reactivemongo.api.Cursor.NoSuchResultException => Set.empty
    }

  def markRoundAsKnown(roundId: Int): Future[Set[Int]] =
    rawDb.appendToIntArray("GPCompletedRounds", roundId)

  def getLastRoundResult: Future[GpRoundResult] = for {
    knownRounds <- getRoundsKnownList
    last <- getRound(knownRounds.max) if knownRounds.nonEmpty
  } yield last
}
