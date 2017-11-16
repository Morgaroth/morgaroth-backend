package io.github.morgaroth.gpbettingleague

import io.github.morgaroth.base.configuration.SimpleConfig
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SeleniumErrDb(page: String, error: String, date: DateTime, stackTrace: Vector[String])

case class BettingDB(rawDb: SimpleConfig) {
  def saveErrorPage(page: String, t: Throwable) = {
    val date = DateTime.now()
    rawDb.put(s"error-scrapping-${date.toString("yyyy-MM-dd-HH:mm:ss")}", SeleniumErrDb(
      page,
      t.getMessage,
      date,
      t.getStackTrace.toVector.map(_.toString).filter(_.contains("github"))
    ))
  }

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
