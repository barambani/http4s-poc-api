package lib

import cats.Eval
import cats.Eval.always
import cats.arrow.FunctionK
import cats.effect.IO
import monix.eval.{Task => MonixTask}
import monix.execution.Scheduler

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scalaz.concurrent.{Task => ScalazTask}

sealed trait ByNameNaturalTransformation[F[_], G[_]] {

  def apply[A](fa: => F[A]): G[A]

  def functionK: FunctionK[F, G] =
    λ[FunctionK[F, G]](apply(_))
}

object ByNameNaturalTransformation {

  type ~~>[F[_], G[_]] = ByNameNaturalTransformation[F, G]

  implicit def futureToIo(implicit ec: ExecutionContext): ByNameNaturalTransformation[Future, IO] =
    new ByNameNaturalTransformation[Future, IO] {
      def apply[A](fa: => Future[A]): IO[A] =
        IO.fromFuture(IO.eval(always(fa)))
    }

  implicit def monixTaskToIo(implicit s: Scheduler): ByNameNaturalTransformation[MonixTask, IO] =
    new ByNameNaturalTransformation[MonixTask, IO] {
      def apply[A](fa: => MonixTask[A]): IO[A] = fa.toIO
    }

  implicit def scalazTaskToIo: ByNameNaturalTransformation[ScalazTask, IO] =
    new ByNameNaturalTransformation[ScalazTask, IO] {
      def apply[A](fa: => ScalazTask[A]): IO[A] =
        IO.eval(Eval.always(fa.unsafePerformSync))
    }
}