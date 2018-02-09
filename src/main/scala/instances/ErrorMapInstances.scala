package instances

import cats.Invariant
import errors.{ApiError, UnknownFailure}
import http4s.extend.Algebra.ExceptionMessage
import http4s.extend.ErrorInvariantMap
import http4s.extend.instances.errorInvariantMap._
import http4s.extend.syntax.invariant._

object ErrorMapInstances {

  implicit def throwableToApiError(implicit ev: Invariant[ErrorInvariantMap[Throwable, ?]]): ErrorInvariantMap[Throwable, ApiError] =
    ErrorInvariantMap[Throwable, ExceptionMessage].imap[ApiError](UnknownFailure)(ae => new ExceptionMessage(ae.message))
}
