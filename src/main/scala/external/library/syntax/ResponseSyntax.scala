package external
package library
package syntax

import cats.effect.Sync
import cats.syntax.applicativeError._
import org.http4s.Response

import scala.language.implicitConversions
import scala.reflect.ClassTag

private[syntax] trait ResponseSyntax {
  implicit def responseSyntax[F[_]](r: F[Response[F]]): ResponseOps[F] = new ResponseOps(r)
}

private[syntax] class ResponseOps[F[_]](private val r: F[Response[F]]) extends AnyVal {

  def handlingFailures[E <: Throwable: ClassTag](hf: E => F[Response[F]])(
    implicit ev: Sync[F]
  ): F[Response[F]] =
    r recoverWith { case e: E => hf(e) }
}
