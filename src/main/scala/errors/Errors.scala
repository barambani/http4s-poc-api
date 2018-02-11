package errors

import http4s.extend.Algebra.ExceptionMessage

sealed trait ApiError extends Product with Serializable {
  def message: String
}

final case class InvalidParameters(message: String)                               extends ApiError
final case class InvalidShippingCountry(message: String)                          extends ApiError
final case class DependencyFailure(failingDependency: String, message: String)    extends ApiError
final case class UnknownFailure(em: ExceptionMessage) extends ApiError {
  val message = em.message
}