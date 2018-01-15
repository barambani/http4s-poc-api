package lib

import cats.Eval.always
import cats.arrow.FunctionK
import cats.effect.IO
import monix.eval.Task

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

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

  implicit def taskToIo: ByNameNaturalTransformation[Task, IO] =
    new ByNameNaturalTransformation[Task, IO] {
      def apply[A](fa: => Task[A]): IO[A] = fa.toIO
    }
}