package instances

import cats.MonadError
import cats.effect.IO
import errors.ApiError
import http4s.extend.ErrorInvariantMap
import http4s.extend.syntax.monadError._

object MonadErrorInstances {

  implicit def ioApiError(implicit EC: ErrorInvariantMap[Throwable, ApiError]): MonadError[IO, ApiError] =
    MonadError[IO, Throwable].adaptErrorType[ApiError]
}