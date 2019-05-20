package model

import model.DomainModel._
import DomainModelValSyntax._
import shapeless.tag.@@
import shapeless.tag

import scala.language.implicitConversions

object DomainModelSyntax {
  implicit def domainModelLongSyntax(x: Long)                = new LongOps(x)
  implicit def domainModelStringSyntax(x: String)            = new StringOps(x)
  implicit def domainModelBigDecimalOpsSyntax(x: BigDecimal) = new BigDecimalOps(x)
}

object DomainModelValSyntax {
  implicit def domainModelValSyntax[A](a: A) = new DomainModelValOps[A](a)
}

final class LongOps(x: Long) {
  def asUserId: UserId       = x.refined[UserIdT]
  def asProductId: ProductId = x.refined[ProductIdT]
}

final class StringOps(x: String) {
  def asCountry: Country         = x.refined[CountryT]
  def asUserAddress: UserAddress = x.refined[UserAddressT]
  def asCurrency: Currency       = x.refined[CurrencyT]
  def asProductSpec: ProductSpec = x.refined[ProductSpecT]
}

final class BigDecimalOps(x: BigDecimal) {
  def asMoneyAmount: MoneyAmount = x.refined[MoneyAmountT]
}

final class DomainModelValOps[A](a: A) {
  def refined[T]: A @@ T = tag[T](a)
}
