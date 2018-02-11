package instances

import cats.Show
import errors._

object ShowInstances {

  implicit val invalidParamsShow: Show[InvalidParameters] =
    new Show[InvalidParameters] {
      def show(t: InvalidParameters): String =
        s"Service Error: InvalidParameters: ${t.message}"
    }

  implicit val invalidShippingShow: Show[InvalidShippingCountry] =
    new Show[InvalidShippingCountry] {
      def show(t: InvalidShippingCountry): String =
        s"Service Error: InvalidShippingCountry: ${t.message}"
    }

  implicit val dependencyFailureShow: Show[DependencyFailure] =
    new Show[DependencyFailure] {
      def show(t: DependencyFailure): String =
        s"Service Error: DependencyFailure. The dependency ${t.failingDependency} failed with message ${t.message}"
    }

  implicit val unknownFailureShow: Show[UnknownFailure] =
    new Show[UnknownFailure] {
      def show(t: UnknownFailure): String =
        s"Service Error: UnknownFailure with message ${t.message}"
    }
}
