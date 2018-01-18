package lib.syntax

import lib.ErrorAdapt

trait ErrorAdaptSyntax {
  implicit final class ErrorAdaptSyntaxOps[F[_], A](anFa: => F[A])(implicit F: ErrorAdapt[F]) {
    def adaptError[E](errM: Throwable => E): F[Either[E, A]] =
      F.adaptError(anFa)(errM)
  }
}

object ErrorAdaptSyntax extends ErrorAdaptSyntax