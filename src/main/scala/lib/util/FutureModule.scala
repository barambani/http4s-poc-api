package lib.util

import cats.syntax.either._

import scala.concurrent.{ExecutionContext, Future}

trait FutureModule {

  def adaptError[E, A](aFuture: Future[A])(errM: Throwable => E)(implicit ec: ExecutionContext): Future[Either[E, A]] =
    aFuture map (_.asRight[E]) recover { case e: Throwable => errM(e).asLeft }
}

object FutureModule extends FutureModule