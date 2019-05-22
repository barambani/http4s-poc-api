package external
package library
package syntax

import org.http4s.Response

import scala.language.implicitConversions

private[syntax] trait ErrorResponseSyntax {
  implicit def errorResponseSyntax[E](e: E): ErrorResponseOps[E] = new ErrorResponseOps(e)
}

private[syntax] class ErrorResponseOps[E](private val e: E) extends AnyVal {
  def responseFor[F[_]](implicit ev: ErrorResponse[F, E]): F[Response[F]] =
    ev.responseFor(e)
}
