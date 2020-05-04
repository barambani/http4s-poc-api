package service

import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import cats.{Monad, Parallel}
import integration.ProductIntegration
import log.effect.LogWriter
import model.DomainModel._

sealed trait PriceCalculator[F[_]] {
  def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]]
}

object PriceCalculator {
  @inline def apply[F[_]: Monad: Parallel[*[_]]](
    productStore: ProductIntegration[F],
    logger: LogWriter[F]
  ): PriceCalculator[F] =
    new PriceCalculator[F] {
      def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] =
        prods.toList.parTraverse(userPrice(pref, user))

      private def userPrice: (UserPreferences, User) => Product => F[Price] =
        (prefs, user) =>
          product =>
            for {
              catalogPrice <-
                productStore.productPrice(product)(prefs) <*
                  logger.debug(s"Catalog price of ${product.id} collected")
              userPrice = veryVeryComplexPureCalculation(catalogPrice)(user.userPurchaseHistory)
              _ <- logger.debug(s"Price calculation for product ${product.id} completed")
            } yield userPrice

      private def veryVeryComplexPureCalculation: Price => Seq[UserPurchase] => Price =
        price => _ => price
    }
}
