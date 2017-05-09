package io.github.morgaroth.base

import scala.concurrent.Future
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
}
