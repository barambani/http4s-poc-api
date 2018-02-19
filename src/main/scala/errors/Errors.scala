package errors

import cats.{Monad, Show}
import http4s.extend._
import org.http4s.Response

sealed trait ApiError extends Product with Serializable {
  def message: String
}
object ApiError extends ApiErrorInstances

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
