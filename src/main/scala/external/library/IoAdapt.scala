package external
package library

import cats.arrow.FunctionK
import cats.effect.{ Concurrent, ContextShift, IO }
import external.library.IoAdapt.-->
import zio.{ Task, ZIO }

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

  implicit def catsIoToZioTask(implicit cc: Concurrent[Task]): IO --> Task =
    new IoAdapt[IO, Task] {
      def apply[A]: (=>IO[A]) => Task[A] =
        io => cc.liftIO(io)
    }

  implicit val futureToZioTask: Future --> Task =
    new IoAdapt[Future, Task] {
      def apply[A]: (=>Future[A]) => Task[A] =
        ft => ZIO.fromFuture(ec => ft.map(identity)(ec))
    }

  implicit def futureToIo(implicit cs: ContextShift[IO]): Future --> IO =
    new IoAdapt[Future, IO] {
      def apply[A]: (=>Future[A]) => IO[A] =
        IO.fromFuture[A] _ compose IO.delay
    }
}

object IoAdapt extends IoAdaptInstances {
  type -->[F[_], G[_]] = IoAdapt[F, G]
}
