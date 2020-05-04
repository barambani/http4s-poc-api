package external
package library
package syntax

import cats.MonadError
import external.library.IoAdapt.-->

import scala.language.implicitConversions

private[syntax] trait IoAdaptSyntax {
  implicit def ioAdaptSyntax[F[_], A](fa: =>F[A]): IoAdaptOps[F, A] = new IoAdaptOps(fa)

  implicit def ioAdaptEitherSyntax[F[_], A, E](fa: =>F[Either[E, A]]): IoAdaptEitherOps[F, A, E] =
    new IoAdaptEitherOps(fa)
}

private[syntax] final class IoAdaptOps[F[_], A](fa: =>F[A]) {
  def adaptedTo[G[_]](implicit nt: F --> G): G[A] = nt.apply(fa)
}

private[syntax] class IoAdaptEitherOps[F[_], A, E](private val fa: F[Either[E, A]]) extends AnyVal {
  def liftIntoMonadError[G[_]](implicit nt: F --> G, err: MonadError[G, E]): G[A] =
    (err.rethrow[A, E] _ compose nt.apply)(fa)
}
