package errors

import cats.{Monad, Semigroup, Show}
import http4s.extend.ErrorResponse
import http4s.extend.ExceptionDisplay._
import http4s.extend.util.ThrowableModule._
import org.http4s.Response

private [errors] trait ApiErrorInstances {

  implicit def apiErrorShow: Show[ApiError] =
    new Show[ApiError] {

      def show(t: ApiError): String =
        apiErrorDecomposition(t)

      private def apiErrorDecomposition: ApiError => String = {
        case e: InvalidParameters       => showOf(e)
        case e: DependencyFailure       => showOf(e)
        case e: InvalidShippingCountry  => showOf(e)
        case e: UnknownFailure          => showOf(e)
        case e: ComposedFailure         => showOf(e)
      }

      private def showOf[E <: ApiError](e: E)(implicit ev: Show[E]): String =
        ev.show(e)
    }

  implicit def apiErrorSemigroup: Semigroup[ApiError] =
    new Semigroup[ApiError] {
      def combine(x: ApiError, y: ApiError): ApiError =
        (x, y) match {
          case (ComposedFailure(List(a, b)), ComposedFailure(List(c, d))) => ComposedFailure(List(a, b, c, d))
          case (ComposedFailure(List(a, b)), _) => ComposedFailure(List(a, b, y))
          case (_, ComposedFailure(List(c, d))) => ComposedFailure(List(x, c, d))
          case _ => ComposedFailure(List(x, y))
        }
    }

  implicit def throwableShow: Show[Throwable] =
    new Show[Throwable] {
      def show(t: Throwable): String =
        t match {
          case e: ApiError  => Show[ApiError].show(e)
          case e: Throwable => unMk(fullDisplay(e))
        }
    }

  implicit def throwableResponse[F[_] : Monad]: ErrorResponse[F, Throwable] =
    new ErrorResponse[F, Throwable] {
      val ev = Show[Throwable]
      def responseFor: Throwable => F[Response[F]] =
        e => InternalServerError(ev.show(e))
    }
}
