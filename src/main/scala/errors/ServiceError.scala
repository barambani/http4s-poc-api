package errors

import cats.syntax.either._
import cats.syntax.show._
import cats.{Monad, MonadError, Show}
import http4s.extend.syntax.errorResponse._
import http4s.extend.{ErrorResponse, newtype}
import org.http4s.Response

object MkServiceError extends newtype[ServiceErrorValue] with ServiceErrorInstances

private[errors] sealed trait ServiceErrorInstances {

  import ThrowableInstances._

  implicit val serviceErrorShow: Show[ServiceError] =
    new Show[ServiceError] {
      def show(x: ServiceError): String =
        x.unMk match {
          case Right(e) => e.show
          case Left(e) => e match {
            case Left(ee)  => ee.show
            case Right(ee) => ee.show
          }
        }
    }

  implicit def serviceErrorResponse[F[_] : Monad]: ErrorResponse[F, ServiceError] =
    new ErrorResponse[F, ServiceError] {
      val ev: Show[ServiceError] = Show[ServiceError]
      def responseFor: ServiceError => F[Response[F]] =
       se => se.unMk match {
          case Right(e)  => e.responseFor
          case Left(e) => e match {
            case Left(ee)  => ee.responseFor
            case Right(ee) => ee.responseFor
          }
       }
    }

  implicit def serviceErrorMonadError[F[_]](implicit F: MonadError[F, Throwable]): MonadError[F, ServiceError] =
    new MonadError[F, ServiceError] {

      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        F.flatMap(fa)(f)

      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
        F.tailRecM(a)(f)

      def raiseError[A](e: ServiceError): F[A] =
        F.raiseError (
          e.unMk match {
            case Right(ee)  => ee
            case Left(ee) => ee match {
              case Left(ie)  => ie.unMk
              case Right(ie) => ie.unMk
            }
          }
        )

      def handleErrorWith[A](fa: F[A])(f: ServiceError => F[A]): F[A] =
        F.handleErrorWith(fa)(
          thr => f(
            if (thr.show.startsWith("InvalidShippingCountry")) InvalidShippingCountry.apply(thr).asServiceError
            else if (thr.show.startsWith("DependencyFailure")) DependencyFailure.apply(thr).asServiceError
            else ServiceError(thr.asRight)
          )
        )

      def pure[A](x: A): F[A] =
        F.pure(x)
    }
}
