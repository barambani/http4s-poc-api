package service

import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import cats.{MonadError, Parallel}
import interpreters.{Dependencies, Logger}
import model.DomainModel._

sealed trait PriceCalculator[F[_]] {
  def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]]
}

object PriceCalculator {

  @inline def apply[F[_] : MonadError[?[_], Throwable], P[_] : Parallel[F, ?[_]]](dependencies: Dependencies[F], logger: Logger[F]): PriceCalculator[F] =
    new PriceCalculatorImpl(dependencies, logger)

  private final class PriceCalculatorImpl[F[_] : MonadError[?[_], Throwable], P[_] : Parallel[F, ?[_]]](dep: Dependencies[F], logger: Logger[F])
    extends PriceCalculator[F] {

      def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] =
        prods.toList parTraverse userPrice(pref)(user)

      private def userPrice: UserPreferences => User => Product => F[Price] =
        prefs => user => product => for {
          catalogPrice  <- dep.productPrice(product)(prefs) <* logger.debug(s"Catalog price of ${ product.id } collected")
          userPrice     =  veryVeryComplexPureCalculation(catalogPrice)(user.userPurchaseHistory)
          _             <- logger.debug(s"Price calculation for product ${ product.id } completed")
        } yield userPrice


      private def veryVeryComplexPureCalculation: Price => Seq[UserPurchase] => Price =
        price => _ => price
    }
}