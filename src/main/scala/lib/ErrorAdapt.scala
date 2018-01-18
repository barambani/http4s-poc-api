package lib

import cats.syntax.either._
import monix.eval.{Task => MonixTask}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.concurrent.{Task => ScalazTask}

trait ErrorAdapt[F[_]] {
  def adapt[E, A](fa: F[A])(errM: Throwable => E): F[Either[E, A]]
}

object ErrorAdapt {

  def apply[F[_]](implicit F: ErrorAdapt[F]): ErrorAdapt[F] = F

  implicit def monixTaskErrorAdapt: ErrorAdapt[MonixTask] =
    new ErrorAdapt[MonixTask] {
      def adapt[E, A](fa: MonixTask[A])(errM: Throwable => E): MonixTask[Either[E, A]] =
        fa.attempt map (_ leftMap errM)
    }

  implicit def futureErrorAdapt(implicit ec: ExecutionContext): ErrorAdapt[Future] =
    new ErrorAdapt[Future] {
      def adapt[E, A](fa: Future[A])(errM: Throwable => E): Future[Either[E,A]] =
        fa map (_.asRight[E]) recover { case e: Throwable => errM(e).asLeft }
    }

  implicit def scalazTaskErrorAdapt: ErrorAdapt[ScalazTask] =
    new ErrorAdapt[ScalazTask] {
      def adapt[E, A](fa: ScalazTask[A])(errM: Throwable => E): ScalazTask[Either[E,A]] =
        fa.attempt map (dj => (dj leftMap errM).toEither)
    }
}