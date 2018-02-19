package errors

import cats.effect.IO
import cats.syntax.either._
import cats.{Invariant, MonadError, Semigroup, Semigroupal, Show}
import http4s.extend.instances.errorInvariantMap._
import http4s.extend.syntax.invariant._
import http4s.extend.syntax.monadError._
import http4s.extend.{ErrorInvariantMap, ExceptionDisplay, ParEffectful}

private [errors] trait ApiErrorInstances {

  implicit def throwableToApiError(implicit ev: Invariant[ErrorInvariantMap[Throwable, ?]]): ErrorInvariantMap[Throwable, ApiError] =
    ErrorInvariantMap[Throwable, ExceptionDisplay].imap[ApiError](UnknownFailure.apply)(ae => ExceptionDisplay.mk(ae.message))

  implicit def ioApiError[E](implicit ev: ErrorInvariantMap[Throwable, E]): MonadError[IO, E] =
    MonadError[IO, Throwable].adaptErrorType[E]

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

  implicit def apiErrorSemigroupal(implicit iev: Semigroup[ApiError]): Semigroupal[Either[ApiError, ?]] =
    new Semigroupal[Either[ApiError, ?]] {
      def product[A, B](fa: Either[ApiError, A], fb: Either[ApiError, B]): Either[ApiError, (A, B)] =
        (fa, fb) match {
          case (Right(a), Right(b)) => (a, b).asRight
          case (Left(a) , Left(b))  => iev.combine(a, b).asLeft
          case (Left(a) , _)        => a.asLeft
          case (_       , Left(b))  => b.asLeft
        }
    }

  implicit def eitherParEffectful(implicit iev: Semigroup[ApiError]): ParEffectful[Either[ApiError, ?]] =
    new ParEffectful[Either[ApiError, ?]] {

      val semigroupalEvidence = apiErrorSemigroupal(iev)

      def parMap2[A, B, R](fa: Either[ApiError, A], fb: Either[ApiError, B])(f: (A, B) => R): Either[ApiError, R] =
        semigroupalEvidence.product(fa, fb).map(x => f(x._1, x._2))
    }
}
