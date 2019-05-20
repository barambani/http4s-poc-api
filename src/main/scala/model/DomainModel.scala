package model

import java.time.Instant

import cats.Show
import shapeless.tag.@@

object DomainModel extends DomainModelInstances {

  sealed trait UserIdT
  sealed trait UserAddressT
  sealed trait CountryT

  sealed trait UserPurchaseTimeT

  sealed trait ProductIdT
  sealed trait ProductSpecT
  sealed trait ProductSaleStartT
  sealed trait ProductSaleEndT

  sealed trait MoneyAmountT
  sealed trait CurrencyT
  sealed trait DiscountAmountT
  sealed trait DiscountReasonT

  type UserId      = Long @@ UserIdT
  type UserAddress = String @@ UserAddressT
  type Country     = String @@ CountryT

  type UserPurchaseTime = Instant @@ UserPurchaseTimeT

  type ProductId        = Long @@ ProductIdT
  type ProductSpec      = String @@ ProductSpecT
  type ProductSaleStart = Instant @@ ProductSaleStartT
  type ProductSaleEnd   = Instant @@ ProductSaleEndT

  type MoneyAmount    = BigDecimal @@ MoneyAmountT
  type Currency       = String @@ CurrencyT
  type DiscountAmount = BigDecimal @@ DiscountAmountT
  type DiscountReason = String @@ DiscountReasonT

  final case class Discount(discount: DiscountAmount, discountReason: DiscountReason)
  final case class Price(
    amount: MoneyAmount,
    currency: Currency,
    discount: Option[Discount],
    priceTimeStamp: Instant
  )
  final case class ShipmentDestination(address: UserAddress, country: Country)
  final case class UserPreferences(destination: ShipmentDestination, currency: Currency)
  final case class ProductSale(starts: ProductSaleStart, ends: ProductSaleEnd)
  final case class Product(id: ProductId, productSpec: ProductSpec, sales: Seq[ProductSale])
  final case class UserPurchase(product: Product, purchaseTime: UserPurchaseTime)
  final case class User(id: UserId, userPurchaseHistory: Seq[UserPurchase])

  final case class PricesRequestPayload(userId: UserId, productIds: Seq[ProductId])

  final case class ServiceSignature(
    name: String,
    version: String,
    scalaVersion: String,
    scalaOrganization: String,
    buildTime: String
  )
}

sealed private[model] trait DomainModelInstances {

  implicit val instantShow: Show[Instant] =
    new Show[Instant] {
      def show(t: Instant): String = t.toString
    }

  implicit val bigDecimalShow: Show[BigDecimal] =
    new Show[BigDecimal] {
      def show(t: BigDecimal): String = t.toString
    }

  implicit def taggedShow[A, T](implicit ev: Show[A]): Show[A @@ T] =
    new Show[A @@ T] {
      def show(t: A @@ T): String = ev.show(t)
    }
}
