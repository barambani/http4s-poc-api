package external
package library
package syntax

import cats.MonadError
import cats.syntax.monadError._

import scala.language.implicitConversions

private[syntax] trait ErrorAdaptSyntax {
  implicit def errorAdaptSyntax[F[_], A](anFa: =>F[A]): ErrorAdaptOps[F, A] = new ErrorAdaptOps(anFa)
}

private[syntax] class ErrorAdaptOps[F[_], A](private val anFa: F[A]) extends AnyVal {

  def narrowFailureWith[E <: Throwable](ef: Throwable => E)(implicit ev: MonadError[F, Throwable]): F[A] =
    anFa adaptError { case th: Throwable => ef(th) }

  def narrowFailureTo[E <: Throwable](implicit ev: MonadError[F, Throwable], ef: ThrowableMap[E]): F[A] =
    anFa adaptError { case th: Throwable => ef map th }
}
