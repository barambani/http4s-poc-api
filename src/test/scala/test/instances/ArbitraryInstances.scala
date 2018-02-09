package test.instances

import errors._
import http4s.extend.Algebra.ExceptionMessage
import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryInstances {

  implicit def excMessageArb(implicit ev: Arbitrary[String]): Arbitrary[ExceptionMessage] =
    Arbitrary { ev.arbitrary flatMap (new ExceptionMessage(_)) }

  implicit def apiErrorArb(implicit ev: Arbitrary[String]): Arbitrary[ApiError] =
    Arbitrary { ev.arbitrary flatMap arbitraryErrorInstance }

  private def arbitraryErrorInstance(implicit ev: Arbitrary[ExceptionMessage]): String => Gen[ApiError] =
    m => ev.arbitrary.flatMap {
      em => Gen.oneOf(InvalidParameters(m), InvalidShippingCountry(m), DependencyFailure("test", m), UnknownFailure(em))
    }
}
