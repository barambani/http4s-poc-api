package errors

import cats.effect.util.CompositeException
import cats.{Monad, Semigroup, Show}
import http4s.extend.ErrorResponse
import http4s.extend.ExceptionDisplay._
import http4s.extend.util.ThrowableModule._
import org.http4s.Response

sealed trait ThrowableInstances {

  implicit def throwableShow: Show[Throwable] =
    new Show[Throwable] {
      def show(t: Throwable): String =
        apiErrorDecomposition(t)

      private def apiErrorDecomposition: Throwable => String = {
        case e: InvalidParameters       => showOf(e)
        case e: DependencyFailure       => showOf(e)
        case e: InvalidShippingCountry  => showOf(e)
        case e: CompositeException      => showOf(e)
        case e: Throwable               => unMk(fullDisplay(e))
      }

      private def showOf[E <: Throwable](e: E)(implicit ev: Show[E]): String =
        ev.show(e)
    }

  implicit def compositeFailureShow(implicit ev: Show[Throwable]): Show[CompositeException] =
    new Show[CompositeException] {
      def show(t: CompositeException): String =
        (t.all map ev.show).toList mkString "\n"
    }

  implicit def throwableResponse[F[_] : Monad]: ErrorResponse[F, Throwable] =
    new ErrorResponse[F, Throwable] {
      val ev = Show[Throwable]
      def responseFor: Throwable => F[Response[F]] =
        e => InternalServerError(ev.show(e))
    }

  implicit def throwableSemigroup: Semigroup[Throwable] =
    new Semigroup[Throwable]{
      def combine(x: Throwable, y: Throwable): Throwable =
        CompositeException(x, y, Nil)
    }
}

object ThrowableInstances extends ThrowableInstances