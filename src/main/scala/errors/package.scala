import cats.syntax.either._
import cats.{Monad, MonadError, Show}
import http4s.extend.ErrorResponse
import org.http4s.Response

package object errors {

  type InvalidShippingCountry = InvalidShippingCountry.T
  val InvalidShippingCountry = MkInvalidShippingCountry

  type DependencyFailure = DependencyFailure.T
  val DependencyFailure = MkDependencyFailure

  type ServiceError = Throwable Either (InvalidShippingCountry Either DependencyFailure)

  object ServiceError {

    import ThrowableInstances._

    implicit def serviceErrorShow(
      implicit
        ev1: Show[InvalidShippingCountry],
        ev2: Show[DependencyFailure],
        ev3: Show[Throwable]): Show[ServiceError] =
      new Show[ServiceError] {
        def show(t: ServiceError): String =
          t match {
            case Left(e)  => ev3.show(e)
            case Right(e) => e match {
              case Left(ee)  => ev1.show(ee)
              case Right(ee) => ev2.show(ee)
            }
          }
      }

    implicit def serviceErrorResponse[F[_] : Monad](
      implicit
        ev1: ErrorResponse[F, InvalidShippingCountry],
        ev2: ErrorResponse[F, DependencyFailure],
        ev3: ErrorResponse[F, Throwable]): ErrorResponse[F, ServiceError] =
      new ErrorResponse[F, ServiceError] {
        val ev: Show[ServiceError] = Show[ServiceError]
        def responseFor: ServiceError => F[Response[F]] = {
          case Left(e)  => ev3.responseFor(e)
          case Right(e) => e match {
            case Left(ee)  => ev1.responseFor(ee)
            case Right(ee) => ev2.responseFor(ee)
          }
        }
      }

    implicit def serviceErrorMonadError[F[_]](
      implicit
        F: MonadError[F, Throwable],
        ev1: MonadError[F, InvalidShippingCountry],
        ev2: MonadError[F, DependencyFailure],
        ev3: Show[Throwable]): MonadError[F, ServiceError] =
      new MonadError[F, ServiceError] {
        def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
          F.flatMap(fa)(f)

        def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
          F.tailRecM(a)(f)

        def raiseError[A](e: ServiceError): F[A] =
          F.raiseError (
            e match {
              case Left(ee)  => ee
              case Right(ee) => ee match {
                case Left(ie)  => ie.unMk
                case Right(ie) => ie.unMk
              }
            }
          )

        def handleErrorWith[A](fa: F[A])(f: ServiceError => F[A]): F[A] =
          F.handleErrorWith(fa)(
            thr => f(
              if (ev3.show(thr).startsWith("InvalidShippingCountry")) InvalidShippingCountry.apply(thr).asServiceError
              else if (ev3.show(thr).startsWith("DependencyFailure")) DependencyFailure.apply(thr).asServiceError
              else thr.asLeft
            )
          )

        def pure[A](x: A): F[A] =
          F.pure(x)
      }

    implicit final class ServiceErrorSyntax(val `this`: ServiceError) extends AnyVal {
      def unMk: Throwable = `this`.asInstanceOf[Throwable]
    }
  }
}