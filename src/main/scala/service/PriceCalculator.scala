package service

import cats.Monad
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import external.library.ParallelEffect
import external.library.syntax.parallelEffect._
import interpreters.{ Dependencies, Logger }
import model.DomainModel._

import scala.concurrent.duration.FiniteDuration

sealed trait PriceCalculator[F[_]] {
  def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]]
}

object PriceCalculator {

  @inline def apply[F[_]: Monad: ParallelEffect](
    dependencies: Dependencies[F],
    logger: Logger[F],
    priceFetchTimeout: FiniteDuration
  ): PriceCalculator[F] =
    new PriceCalculatorImpl(dependencies, logger, priceFetchTimeout)

  final private class PriceCalculatorImpl[F[_]: Monad: ParallelEffect](
    dep: Dependencies[F],
    logger: Logger[F],
    priceFetchTimeout: FiniteDuration
  ) extends PriceCalculator[F] {

    def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] =
      prods.toList.parallelTraverse(userPrice(pref)(user))(priceFetchTimeout)

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
