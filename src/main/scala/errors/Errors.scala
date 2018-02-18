package errors

import cats.effect.IO
import cats.syntax.either._
import cats.{Invariant, Monad, MonadError, Semigroup, Semigroupal, Show}
import http4s.extend._
import http4s.extend.instances.errorInvariantMap._
import http4s.extend.syntax.invariant._
import http4s.extend.syntax.monadError._
import org.http4s.Response

sealed trait ApiError extends Product with Serializable {
  def message: String
}
object ApiError {

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

final case class InvalidParameters(message: String) extends ApiError
object InvalidParameters {

  implicit val invalidParamsShow: Show[InvalidParameters] =
    new Show[InvalidParameters] {
      def show(t: InvalidParameters): String =
        s"Service Error: InvalidParameters: ${t.message}"
    }

  implicit def invalidParamsResponse[F[_] : Monad]: ErrorResponse[F, InvalidParameters] =
    new ErrorResponse[F, InvalidParameters] {
      val ev = Show[InvalidParameters]
      def responseFor: InvalidParameters => F[Response[F]] =
        e => BadRequest(ev.show(e))
    }
}

final case class InvalidShippingCountry(message: String) extends ApiError
object InvalidShippingCountry {

  implicit val invalidShippingShow: Show[InvalidShippingCountry] =
    new Show[InvalidShippingCountry] {
      def show(t: InvalidShippingCountry): String =
        s"Service Error: InvalidShippingCountry: ${t.message}"
    }

  implicit def invalidShippingCountryResponse[F[_] : Monad]: ErrorResponse[F, InvalidShippingCountry] =
    new ErrorResponse[F, InvalidShippingCountry] {
      val ev = Show[InvalidShippingCountry]
      def responseFor: InvalidShippingCountry => F[Response[F]] =
        e => InternalServerError(ev.show(e))
    }
}

final case class DependencyFailure(failingDependency: String, message: String) extends ApiError
object DependencyFailure {

  implicit val dependencyFailureShow: Show[DependencyFailure] =
    new Show[DependencyFailure] {
      def show(t: DependencyFailure): String =
        s"Service Error: DependencyFailure. The dependency ${t.failingDependency} failed with message ${t.message}"
    }

  implicit def dependencyFailureResponse[F[_] : Monad]: ErrorResponse[F, DependencyFailure] =
    new ErrorResponse[F, DependencyFailure] {
      val ev = Show[DependencyFailure]
      def responseFor: DependencyFailure => F[Response[F]] =
        e => BadGateway(ev.show(e))
    }
}


final case class UnknownFailure(em: ExceptionDisplay) extends ApiError {
  val message = ExceptionDisplay.unMk(em)
}
object UnknownFailure {

  implicit val unknownFailureShow: Show[UnknownFailure] =
    new Show[UnknownFailure] {
      def show(t: UnknownFailure): String =
        s"Service Error: UnknownFailure with message ${t.message}"
    }

  implicit def unknownFailureResponse[F[_] : Monad]: ErrorResponse[F, UnknownFailure] =
    new ErrorResponse[F, UnknownFailure] {
      val ev = Show[UnknownFailure]
      def responseFor: UnknownFailure => F[Response[F]] =
        e => InternalServerError(ev.show(e))
    }
}

final case class ComposedFailure(errors: List[ApiError]) extends ApiError {

  private val ev = Show[ApiError]
  val message = (errors map {
    m =>
      s"""
         |${ev.show(m)}"""
    } mkString "").stripMargin
}
object ComposedFailure {

  implicit val composedFailureShow: Show[ComposedFailure] =
    new Show[ComposedFailure] {
      def show(t: ComposedFailure): String =
        s"""Service Error: ComposedFailure with messages:${t.message}""".stripMargin
    }

  implicit def composedFailureResponse[F[_] : Monad]: ErrorResponse[F, ComposedFailure] =
    new ErrorResponse[F, ComposedFailure] {
      val ev = Show[ComposedFailure]
      def responseFor: ComposedFailure => F[Response[F]] =
        e => InternalServerError(ev.show(e))
    }
}
