package model
package syntax

import model.DomainModel._
import model.syntax.DomainModelSyntax.{ domainModelTaggedOps, BigDecimalOps, LongOps, StringOps }
import shapeless.tag
import shapeless.tag.@@

import scala.language.implicitConversions

private[syntax] trait DomainModelSyntax {
  implicit def domainModelLongSyntax(x: Long)                = new LongOps(x)
  implicit def domainModelStringSyntax(x: String)            = new StringOps(x)
  implicit def domainModelBigDecimalOpsSyntax(x: BigDecimal) = new BigDecimalOps(x)
  implicit def domainModelTaggedSyntax[A](a: A)              = new domainModelTaggedOps[A](a)
}

private[syntax] object DomainModelSyntax {
  import syntax.domainModel._

  final class LongOps(private val x: Long) extends AnyVal {
    def asUserId: UserId       = x.refined[UserIdT]
    def asProductId: ProductId = x.refined[ProductIdT]
  }

  final class StringOps(private val x: String) extends AnyVal {
    def asCountry: Country         = x.refined[CountryT]
    def asUserAddress: UserAddress = x.refined[UserAddressT]
    def asCurrency: Currency       = x.refined[CurrencyT]
    def asProductSpec: ProductSpec = x.refined[ProductSpecT]
  }

  final class BigDecimalOps(private val x: BigDecimal) extends AnyVal {
    def asMoneyAmount: MoneyAmount = x.refined[MoneyAmountT]
  }

  final class domainModelTaggedOps[A](private val a: A) extends AnyVal {
    def refined[T]: A @@ T = tag[T](a)
  }
}
