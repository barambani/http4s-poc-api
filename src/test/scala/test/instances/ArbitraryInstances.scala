package test.instances

import errors._
import http4s.extend.ExceptionDisplay
import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryInstances {

  implicit def excMessageArb(implicit ev: Arbitrary[String]): Arbitrary[ExceptionDisplay] =
    Arbitrary { ev.arbitrary flatMap (ExceptionDisplay(_)) }

  implicit def apiErrorArb(implicit ev: Arbitrary[String]): Arbitrary[ApiError] =
    Arbitrary { ev.arbitrary flatMap arbitraryErrorInstance }

  private def arbitraryErrorInstance(implicit ev: Arbitrary[ExceptionDisplay]): String => Gen[ApiError] =
    m => ev.arbitrary.flatMap {
      em => Gen.oneOf(
        InvalidParameters(m),
        InvalidShippingCountry(m),
        DependencyFailure("test", m),
        UnknownFailure(em),
        ComposedFailure(
          List(InvalidParameters(m), DependencyFailure("test", m), UnknownFailure(em))
        )
      )
    }
}
