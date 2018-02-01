package errors

sealed trait ApiError extends Product with Serializable {
  def message: String

  override def toString: String =
    this match {
      case InvalidParameters(m)       => s"Service Error: InvalidParameters: $m"
      case InvalidShippingCountry(m)  => s"Service Error: InvalidShippingCountry: $m"
      case DependencyFailure(fd, m)   => s"Service Error: DependencyFailure. The dependency $fd failed with message $m"
      case UnknownFailure(m)          => s"Service Error: UnknownFailure with message $m"
    }
}

final case class InvalidParameters(message: String)                             extends ApiError
final case class InvalidShippingCountry(message: String)                        extends ApiError
final case class DependencyFailure(failingDependency: String, message: String)  extends ApiError
final case class UnknownFailure(message: String)                                extends ApiError