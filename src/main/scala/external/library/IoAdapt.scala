package external
package library

import cats.Eval.always
import cats.arrow.FunctionK
import cats.effect.IO
import cats.instances.future.catsStdInstancesForFuture
import cats.{ Eval, Functor }
import external.library.IoAdapt.~~>
import monix.eval.{ Task => MonixTask }
import monix.execution.Scheduler
import scalaz.concurrent.{ Task => ScalazTask }

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Models a natural transformation between the Functors `F[_]` and `G[_]`.
  */
sealed trait IoAdapt[F[_], G[_]] {

  implicit def evF: Functor[F]
  implicit def evG: Functor[G]

  /**
    * Gives the Natural Transformation from `F` to `G` for all the types `A` where `F` is called by name
    */
  def apply[A]: (=>F[A]) => G[A]

  def functionK: FunctionK[F, G] =
    λ[FunctionK[F, G]](apply(_))
}

sealed private[library] trait IoAdaptInstances {

  implicit def futureToIo(implicit ec: ExecutionContext): Future ~~> IO =
    new IoAdapt[Future, IO] {
      val evF = Functor[Future]
      val evG = Functor[IO]

      def apply[A]: (=>Future[A]) => IO[A] =
        IO.fromFuture[A] _ compose IO.eval[Future[A]] compose always
    }

  implicit def monixTaskToIo(implicit s: Scheduler): MonixTask ~~> IO =
    new IoAdapt[MonixTask, IO] {
      val evF = Functor[MonixTask]
      val evG = Functor[IO]

      def apply[A]: (=>MonixTask[A]) => IO[A] = _.toIO
    }

  implicit def scalazTaskToIo: ScalazTask ~~> IO =
    new IoAdapt[ScalazTask, IO] {
      val evF = scalazTaskFunctor
      val evG = Functor[IO]

      def apply[A]: (=>ScalazTask[A]) => IO[A] =
        _.unsafePerformSyncAttempt.fold(
          e => IO.raiseError(e),
          a => IO.pure(a)
        )
    }

  implicit def ioToScalazTask: IO ~~> ScalazTask =
    new IoAdapt[IO, ScalazTask] {
      val evF = Functor[IO]
      val evG = scalazTaskFunctor

      def apply[A]: (=>IO[A]) => ScalazTask[A] =
        fa =>
          Eval
            .always[ScalazTask[A]](
              fa.attempt.unsafeRunSync.fold(ScalazTask.fail, a => ScalazTask.delay(a))
            )
            .value
    }

  private def scalazTaskFunctor: Functor[ScalazTask] =
    new Functor[ScalazTask] {
      def map[A, B](fa: ScalazTask[A])(f: A => B): ScalazTask[B] = fa map f
    }
}

object IoAdapt extends IoAdaptInstances {
  type ~~>[F[_], G[_]] = IoAdapt[F, G]
}
