package errors

import cats.syntax.show._
import com.github.ghik.silencer.silent
import external.library.ThrowableMap
import external.library.instances.throwable._
import shapeless.{::, Generic, HNil}

sealed trait PriceServiceError extends Exception with Product with Serializable

object PriceServiceError {
  final case class UserErr(reason: String)                extends PriceServiceError
  final case class PreferenceErr(reason: String)          extends PriceServiceError
  final case class ProductErr(reason: String)             extends PriceServiceError
  final case class ProductPriceErr(reason: String)        extends PriceServiceError
  final case class InvalidShippingCountry(reason: String) extends PriceServiceError
  final case class CacheLookupError(reason: String)       extends PriceServiceError
  final case class CacheStoreError(reason: String)        extends PriceServiceError

  @silent implicit def stringThrowableMap[A](
    implicit ev: A <:< PriceServiceError,
    gen: Generic.Aux[A, String :: HNil]
  ): ThrowableMap[A] =
    new ThrowableMap[A] {
      def map(th: Throwable): A =
        gen from (th.show :: HNil)
    }
}
