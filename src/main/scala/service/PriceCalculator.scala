package service

import cats.Monad
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import http4s.extend.ParEffectful
import http4s.extend.syntax.parEffectful._
import interpreters.{ Dependencies, Logger }
import model.DomainModel._

sealed trait PriceCalculator[F[_]] {
  def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]]
}

object PriceCalculator {

  @inline def apply[F[_]: Monad: ParEffectful](
    dependencies: Dependencies[F],
    logger: Logger[F]
  ): PriceCalculator[F] =
    new PriceCalculatorImpl(dependencies, logger)

  final private class PriceCalculatorImpl[F[_]: Monad: ParEffectful](dep: Dependencies[F], logger: Logger[F])
      extends PriceCalculator[F] {

    def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] =
      prods.toList parTraverse userPrice(pref)(user)

    private def userPrice: UserPreferences => User => Product => F[Price] =
      prefs =>
        user =>
          product =>
            for {
              catalogPrice <- dep.productPrice(product)(prefs) <* logger.debug(
                               s"Catalog price of ${product.id} collected"
                             )
              userPrice = veryVeryComplexPureCalculation(catalogPrice)(user.userPurchaseHistory)
              _         <- logger.debug(s"Price calculation for product ${product.id} completed")
            } yield userPrice

    private def veryVeryComplexPureCalculation: Price => Seq[UserPurchase] => Price =
      price => _ => price
  }
}
