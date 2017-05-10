package io.github.morgaroth.base

import akka.event.LoggingAdapter

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.Try

/**
  * Created by PRV on 08.05.2017.
  */
object FutureHelpers {

  def future[T](value: Throwable): Future[T] = Future.failed[T](value)

  def future[T](value: T): Future[T] = Future.successful(value)

  def future[T](value: Try[T]): Future[T] = Future.fromTry(value)

  def succ[T](value: T): Future[T] = Future.successful(value)

  def fail[T](value: Throwable): Future[T] = Future.failed[T](value)

  implicit def wrapToRich[T](f: Future[T]): RichFuture[T] = RichFuture[T](f)
}

case class RichFuture[T](f: Future[T]) {
  def logErrors(message: String, args: Any*)(implicit log: LoggingAdapter, ex: ExecutionContext) = {
    f.failed.foreach {
      thr => log.error(thr, log.format(message, args))
    }
    f
  }

  def logOut(pf: PartialFunction[Try[T], Unit])(implicit log: LoggingAdapter, ex: ExecutionContext) = {
    f.onComplete(pf)
    f
  }
}
