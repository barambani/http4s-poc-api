package errors

import cats.{Monad, Show}
import http4s.extend._
import org.http4s.Response

final case class InvalidParameters(message: String) extends Throwable(message)
object InvalidParameters {

  implicit val invalidParamsShow: Show[InvalidParameters] =
    new Show[InvalidParameters] {
      def show(t: InvalidParameters): String =
        s"InvalidParameters: ${t.message}"
    }

  implicit def invalidParamsResponse[F[_] : Monad]: ErrorResponse[F, InvalidParameters] =
    new ErrorResponse[F, InvalidParameters] {
      val ev = Show[InvalidParameters]
      def responseFor: InvalidParameters => F[Response[F]] =
        e => BadRequest(ev.show(e))
    }
}

final case class InvalidShippingCountry(message: String) extends Throwable(message)
object InvalidShippingCountry {

  implicit val invalidShippingShow: Show[InvalidShippingCountry] =
    new Show[InvalidShippingCountry] {
      def show(t: InvalidShippingCountry): String =
        s"InvalidShippingCountry: ${t.message}"
    }

  implicit def invalidShippingCountryResponse[F[_] : Monad]: ErrorResponse[F, InvalidShippingCountry] =
    new ErrorResponse[F, InvalidShippingCountry] {
      val ev = Show[InvalidShippingCountry]
      def responseFor: InvalidShippingCountry => F[Response[F]] =
        e => InternalServerError(ev.show(e))
    }
}

final case class DependencyFailure(failingDependency: String, message: String)
  extends Throwable(s"Failed dependency: $failingDependency. Message: $message")
object DependencyFailure {

  implicit val dependencyFailureShow: Show[DependencyFailure] =
    new Show[DependencyFailure] {
      def show(t: DependencyFailure): String =
        s"DependencyFailure. The dependency ${t.failingDependency} failed with message ${t.message}"
    }

  implicit def dependencyFailureResponse[F[_] : Monad]: ErrorResponse[F, DependencyFailure] =
    new ErrorResponse[F, DependencyFailure] {
      val ev = Show[DependencyFailure]
      def responseFor: DependencyFailure => F[Response[F]] =
        e => BadGateway(ev.show(e))
    }
}