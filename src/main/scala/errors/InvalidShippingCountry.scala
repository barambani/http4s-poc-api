package errors

import cats.syntax.either._
import cats.{Monad, MonadError, Show}
import external.library.{ErrorResponse, newtype}
import org.http4s.Response

object MkInvalidShippingCountry extends newtype[Throwable] with InvalidShippingCountryInstances {

  implicit final class InvalidShippingCountryOps(val `this`: InvalidShippingCountry) extends AnyVal {
    def asServiceError: ServiceError = ServiceError(`this`.asLeft.asLeft)
  }
}

sealed private[errors] trait InvalidShippingCountryInstances {

  implicit def invalidShippingShow(implicit ev: Show[Throwable]): Show[InvalidShippingCountry] =
    new Show[InvalidShippingCountry] {
      def show(t: InvalidShippingCountry): String = ev.show(t.unMk)
    }

  implicit def invalidShippingCountryResponse[F[_]: Monad](
    implicit sh: Show[Throwable]
  ): ErrorResponse[F, InvalidShippingCountry] =
    new ErrorResponse[F, InvalidShippingCountry] {
      val ev = Show[InvalidShippingCountry]
      def responseFor: InvalidShippingCountry => F[Response[F]] =
        e => InternalServerError(ev.show(e))
    }

  implicit def invalidShippingCountryMonadError[F[_]](
    implicit F: MonadError[F, Throwable]
  ): MonadError[F, InvalidShippingCountry] =
    new MonadError[F, InvalidShippingCountry] {
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        F.flatMap(fa)(f)

      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
        F.tailRecM(a)(f)

      def raiseError[A](e: InvalidShippingCountry): F[A] =
        F.raiseError(e.unMk)

      def handleErrorWith[A](fa: F[A])(f: InvalidShippingCountry => F[A]): F[A] =
        F.handleErrorWith(fa)(f compose InvalidShippingCountry.apply)

      def pure[A](x: A): F[A] =
        F.pure(x)
    }
}
