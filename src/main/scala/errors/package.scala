import external.library.|

package object errors {

  type ServiceErrorValue =
    InvalidShippingCountry | DependencyFailure | Throwable

  type InvalidShippingCountry = InvalidShippingCountry.T
  val InvalidShippingCountry = MkInvalidShippingCountry

  type DependencyFailure = DependencyFailure.T
  val DependencyFailure = MkDependencyFailure

  type ServiceError = ServiceError.T
  val ServiceError = MkServiceError
}
