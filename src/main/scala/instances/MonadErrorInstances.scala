package instances

import cats.MonadError
import cats.effect.IO
import errors.ApiError
import http4s.extend.ErrorInvariantMap
import http4s.extend.syntax.MonadErrorModuleSyntax._

object MonadErrorInstances {

  implicit def ioServiceError(implicit EC: ErrorInvariantMap[Throwable, ApiError]): MonadError[IO, ApiError] =
    MonadError[IO, Throwable].adaptErrorType[ApiError]
}