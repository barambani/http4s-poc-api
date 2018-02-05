package test.instances

import errors._
import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryInstances {

  implicit def testErrorArb(implicit ev: Arbitrary[String]): Arbitrary[ApiError] =
    Arbitrary { ev.arbitrary flatMap arbitraryErrorInstance }

  private def arbitraryErrorInstance: String => Gen[ApiError] =
    m => Gen.oneOf(InvalidParameters(m), InvalidShippingCountry(m), DependencyFailure("test", m), UnknownFailure(m))
}
