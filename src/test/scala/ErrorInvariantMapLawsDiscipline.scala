import cats.Eq
import cats.tests.CatsSuite
import errors._
import instances.ErrorMapInstances._
import laws.checks.ErrorInvariantMapLawsChecks
import org.scalacheck.{Arbitrary, Gen}

final class ErrorInvariantMapLawsDiscipline extends CatsSuite {

  implicit val throwableEq: Eq[Throwable] =
    Eq.by[Throwable, String](_.getMessage)

  implicit val apiErrorEq: Eq[ApiError] =
    Eq.by[ApiError, String](_.message)

  implicit def testErrorArb(implicit AI: Arbitrary[String]): Arbitrary[ApiError] =
    Arbitrary { AI.arbitrary flatMap arbitraryErrorInstance }

  private def arbitraryErrorInstance: String => Gen[ApiError] =
    m => Gen.oneOf(InvalidParameters(m), InvalidShippingCountry(m), DependencyFailure("test", m), UnknownFailure(m))

  checkAll(
    "ErrorInvariantMapLawsChecks[Throwable, TestError]",
    ErrorInvariantMapLawsChecks[Throwable, ApiError].errorInvariantMap
  )
}
