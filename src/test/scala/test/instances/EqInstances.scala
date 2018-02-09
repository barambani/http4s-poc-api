package test.instances

import cats.Eq
import cats.instances.string._
import errors.ApiError

trait EqInstances {

  implicit def apiErrorEq: Eq[ApiError] =
    Eq.by[ApiError, String](_.message)
}
