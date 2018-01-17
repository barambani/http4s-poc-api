package service

import cats.MonadError
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.traverse._
import errors.ApiError
import model.DomainModel._

import scala.language.higherKinds

sealed trait PriceCalculator[F[_]] {
  def finalPrice(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]]
}

final case class PriceCalculatorImpl[F[_] : MonadError[?[_], ApiError]](dep: Dependencies[F], logger: Logger[F]) extends PriceCalculator[F] {

  // TODO: Run in parallel in F
  def finalPrice(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] =
    (prods.toList map userPrice(pref)(user)).sequence

  private def userPrice: UserPreferences => User => Product => F[Price] =
    prefs => user => product => for {
      catalogPrice <- dep.productPrice(product)(prefs) <* logger.info(s"Catalog price of ${product.id} collected")
      userPrice     = veryVeryComplexCalculation(catalogPrice)(user.userPurchaseHistory)
    } yield userPrice

  private def veryVeryComplexCalculation: Price => Seq[UserPurchase] => Price =
    price => _ => price
}