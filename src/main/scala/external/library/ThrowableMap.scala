package external
package library

import cats.syntax.show._
import com.github.ghik.silencer.silent
import errors.PriceServiceError
import shapeless.{ ::, Generic, HNil }

trait ThrowableMap[E] { self =>
  def map(th: Throwable): E
}

object ThrowableMap extends ThrowableInstances {

  @silent implicit def stringThrowableMap[A, Gen](
    implicit
    ev1: A <:< PriceServiceError,
    gen: Generic.Aux[A, String :: HNil],
  ): ThrowableMap[A] =
    new ThrowableMap[A] {
      def map(th: Throwable): A =
        gen from (th.show :: HNil)
    }
}
