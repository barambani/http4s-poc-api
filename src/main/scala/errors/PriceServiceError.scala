package errors

sealed trait PriceServiceError extends Exception with Product with Serializable

object PriceServiceError {

  final case class UserErr(reason: String)                extends PriceServiceError
  final case class PreferenceErr(reason: String)          extends PriceServiceError
  final case class ProductErr(reason: String)             extends PriceServiceError
  final case class ProductPriceErr(reason: String)        extends PriceServiceError
  final case class InvalidShippingCountry(reason: String) extends PriceServiceError
  final case class CacheLookupError(reason: String)       extends PriceServiceError
  final case class CacheStoreError(reason: String)        extends PriceServiceError
}
