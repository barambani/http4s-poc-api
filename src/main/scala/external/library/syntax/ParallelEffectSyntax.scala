package external
package library
package syntax

import cats.{ Applicative, Monoid, Traverse }

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

private[syntax] trait ParallelEffectSyntax {

  implicit def parallelEffectSyntax[F[_], A](fa: F[A]): ParallelEffectOps[F, A] =
    new ParallelEffectOps(fa)

  implicit def parallelEffectTraverseSyntax[T[_]: Traverse: Applicative, A](
    t: T[A]
  ): ParallelEffectTraverseOps[T, A] =
    new ParallelEffectTraverseOps(t)
}

private[syntax] class ParallelEffectOps[F[_], A](private val fa: F[A]) extends AnyVal {

  def inParallelWith[B](fb: =>F[B])(t: FiniteDuration)(implicit F: ParallelEffect[F]): F[(A, B)] =
    F.parallelRun(fa, fb)(t)

  /**
    * Alias for parallelRun
    *
    * - Example:
    * (F.delay(1) <||> F.delay(2))(1.second)
    */
  def <||>[B](fb: =>F[B])(t: FiniteDuration)(implicit F: ParallelEffect[F]): F[(A, B)] =
    inParallelWith(fb)(t)

  /**
    * Equivalent to *> but in parallel
    *
    * - Example:
    * (F.unit *||> F.delay(1))(1.second)
    */
  def *||>[B](fb: =>F[B])(t: FiniteDuration)(implicit F: ParallelEffect[F]): F[B] =
    F.parallelMap2(fa, fb)(t)((_, b) => b)

  /**
    * Equivalent to <* but in parallel
    *
    * - Example:
    * (F.delay(1) <||* F.unit)(1.second)
    */
  def <||*[B](fb: =>F[B])(t: FiniteDuration)(implicit F: ParallelEffect[F]): F[A] =
    F.parallelMap2(fa, fb)(t)((a, _) => a)
}

private[syntax] class ParallelEffectTraverseOps[T[_], A](private val traverse: T[A]) extends AnyVal {

  def parallelTraverse[F[_]: ParallelEffect: Applicative, B](f: A => F[B])(t: FiniteDuration)(
    implicit
    ev1: Monoid[T[B]],
    ev2: Traverse[T],
    ev3: Applicative[T]
  ): F[T[B]] =
    ParallelEffect.parallelTraverse(traverse)(f)(t)
}
