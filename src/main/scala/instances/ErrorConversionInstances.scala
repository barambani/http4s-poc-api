package instances

import errors.{ApiError, UnknownFailure}
import http4s.extend.ErrorInvariantMap

object ErrorConversionInstances {

  implicit val throwableToServiceError: ErrorInvariantMap[Throwable, ApiError] =
    new ErrorInvariantMap[Throwable, ApiError] {

      def direct: Throwable => ApiError =
        e => UnknownFailure(e.getMessage)

      def reverse: ApiError => Throwable =
        e => new Throwable(e.toString)
    }
}