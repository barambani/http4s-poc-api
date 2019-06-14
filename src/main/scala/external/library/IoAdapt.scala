package external
package library

import cats.Eval
import cats.arrow.FunctionK
import cats.effect.{ ContextShift, IO }
import external.library.IoAdapt.-->
import monix.eval.{ Task => MonixTask }
import monix.execution.Scheduler
import scalaz.concurrent.{ Task => ScalazTask }

import scala.concurrent.Future

/**
  * Models a natural transformation between the Functors `F[_]` and `G[_]`.
  */
sealed trait IoAdapt[F[_], G[_]] {

  /**
    * Gives the Natural Transformation from `F` to `G` for all the types `A` where `F` is called by name
    */
  def apply[A]: (=>F[A]) => G[A]

  def functionK: FunctionK[F, G] =
    λ[FunctionK[F, G]](apply(_))
}

sealed private[library] trait IoAdaptInstances {

  implicit def futureToIo(implicit cs: ContextShift[IO]): Future --> IO =
    new IoAdapt[Future, IO] {
      def apply[A]: (=>Future[A]) => IO[A] =
        IO.fromFuture[A] _ compose IO.delay
    }

  implicit def monixTaskToIo(implicit s: Scheduler): MonixTask --> IO =
    new IoAdapt[MonixTask, IO] {
      def apply[A]: (=>MonixTask[A]) => IO[A] = _.toIO
    }

  implicit def scalazTaskToIo: ScalazTask --> IO =
    new IoAdapt[ScalazTask, IO] {
      def apply[A]: (=>ScalazTask[A]) => IO[A] =
        _.unsafePerformSyncAttempt.fold(
          e => IO.raiseError(e),
          a => IO.pure(a)
        )
    }

  implicit def ioToScalazTask: IO --> ScalazTask =
    new IoAdapt[IO, ScalazTask] {
      def apply[A]: (=>IO[A]) => ScalazTask[A] =
        fa =>
          Eval
            .always[ScalazTask[A]](
              fa.attempt.unsafeRunSync.fold(ScalazTask.fail, a => ScalazTask.delay(a))
            )
            .value
    }
}

object IoAdapt extends IoAdaptInstances {
  type -->[F[_], G[_]] = IoAdapt[F, G]
}
