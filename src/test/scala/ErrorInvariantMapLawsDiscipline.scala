import cats.tests.CatsSuite
import errors._
import http4s.extend.instances.eq._
import http4s.extend.instances.invariant._
import laws.checks.ErrorInvariantMapLawsChecks
import test.instances.{ArbitraryInstances, EqInstances}

final class ErrorInvariantMapLawsDiscipline extends CatsSuite with EqInstances with ArbitraryInstances {

  checkAll(
    "ErrorInvariantMapLawsChecks[Throwable, ApiError]",
    ErrorInvariantMapLawsChecks[Throwable, ApiError].errorInvariantMap
  )
}
