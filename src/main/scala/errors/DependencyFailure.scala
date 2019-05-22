package errors

import cats.syntax.either._
import cats.{ Monad, MonadError, Show }
import external.library.{ newtype, ErrorResponse }
import org.http4s.Response

object MkDependencyFailure extends newtype[Throwable] with DependencyFailureInstances {

  implicit final class InvalidShippingCountryOps(val `this`: DependencyFailure) extends AnyVal {
    def asServiceError: ServiceError = ServiceError(`this`.asRight.asLeft)
  }
}

sealed private[errors] trait DependencyFailureInstances {

  implicit def dependencyFailureShow(implicit ev: Show[Throwable]): Show[DependencyFailure] =
    new Show[DependencyFailure] {
      def show(t: DependencyFailure): String = ev.show(t.unMk)
    }

  implicit def dependencyFailureResponse[F[_]: Monad](
    implicit sh: Show[Throwable]
  ): ErrorResponse[F, DependencyFailure] =
    new ErrorResponse[F, DependencyFailure] {
      val ev = Show[DependencyFailure]
      def responseFor: DependencyFailure => F[Response[F]] =
        e => BadGateway(ev.show(e))
    }

  implicit def dependencyFailureMonadError[F[_]](
    implicit F: MonadError[F, Throwable]
  ): MonadError[F, DependencyFailure] =
    new MonadError[F, DependencyFailure] {
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        F.flatMap(fa)(f)

      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
        F.tailRecM(a)(f)

      def raiseError[A](e: DependencyFailure): F[A] =
        F.raiseError(e.unMk)

      def handleErrorWith[A](fa: F[A])(f: DependencyFailure => F[A]): F[A] =
        F.handleErrorWith(fa)(f compose DependencyFailure.apply)

      def pure[A](x: A): F[A] =
        F.pure(x)
    }
}
