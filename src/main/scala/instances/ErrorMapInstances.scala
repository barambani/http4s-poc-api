package instances

import errors.{ApiError, UnknownFailure}
import http4s.extend.ErrorInvariantMap

object ErrorMapInstances {

  implicit val throwableToApiError: ErrorInvariantMap[Throwable, ApiError] =
    new ErrorInvariantMap[Throwable, ApiError] {

      def direct: Throwable => ApiError =
        e => UnknownFailure(e.getMessage)

      def reverse: ApiError => Throwable =
        e => new Throwable(e.message)
    }
}