package external
package library
package syntax

import scala.language.implicitConversions

private[syntax] trait ErrorAdaptSyntax {
  implicit def errorAdaptSyntax[F[_], A](anFa: =>F[A]): ErrorAdaptOps[F, A] = new ErrorAdaptOps(anFa)
}

private[syntax] class ErrorAdaptOps[F[_], A](val anFa: F[A]) extends AnyVal {
  def attemptMapLeft[E](errM: Throwable => E)(implicit F: ErrorAdapt[F]): F[Either[E, A]] =
    F.attemptMapLeft(anFa)(errM)
}
