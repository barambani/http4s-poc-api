package test.instances

import errors.ApiError
import org.scalacheck.Cogen

trait CogenInstances {

  implicit def apiErrorCogen(implicit ev: Cogen[String]): Cogen[ApiError] =
    ev contramap (_.message)
}
