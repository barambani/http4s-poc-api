import cats.effect.IO
import cats.effect.laws.discipline.arbitrary._
import cats.laws.discipline.MonadErrorTests
import cats.tests.CatsSuite
import errors.ApiError
import http4s.extend.instances.eq._
import http4s.extend.instances.invariant._
import instances.ErrorMapInstances._
import instances.MonadErrorInstances._
import test.instances.{ArbitraryInstances, CogenInstances, EqInstances}

final class MonadErrorLawsDiscipline extends CatsSuite with ArbitraryInstances with CogenInstances with EqInstances {

  checkAll(
    "MonadErrorTests[IO, ApiError]",
    MonadErrorTests[IO, ApiError].monadError[Boolean, Int, String]
  )
}