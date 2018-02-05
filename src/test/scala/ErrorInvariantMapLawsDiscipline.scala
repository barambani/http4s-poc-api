import cats.tests.CatsSuite
import errors._
import instances.ErrorMapInstances._
import laws.checks.ErrorInvariantMapLawsChecks
import test.instances.{ArbitraryInstances, EqInstances}

final class ErrorInvariantMapLawsDiscipline extends CatsSuite with EqInstances with ArbitraryInstances {

  checkAll(
    "ErrorInvariantMapLawsChecks[Throwable, ApiError]",
    ErrorInvariantMapLawsChecks[Throwable, ApiError].errorInvariantMap
  )
}
