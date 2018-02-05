package test.instances

import cats.Eq
import cats.instances.string._
import errors.ApiError

trait EqInstances {

  implicit def throwableEq: Eq[Throwable] =
    Eq.by[Throwable, String](_.getMessage)

  implicit def apiErrorEq: Eq[ApiError] =
    Eq.by[ApiError, String](_.message)
}
