package service

import cats.MonadError
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.ApiError
import model.DomainModel._

import scala.language.higherKinds

final case class PriceService[F[_] : MonadError[?[_], ApiError]](dep: Dependencies[F], logger: Logger[F]) {

  def prices(userId: UserId, productIds: Seq[ProductId]): F[Seq[Price]] =
    for {
      user            <- dep.user(userId)                           <* logger.info(s"User collected for $userId")
      preferences     <- preferenceFetcher.userPreferences(userId)  <* logger.info(s"User preferences collected for $userId")
      products        <- productRepo.storedProducts(productIds)     <* logger.info(s"Product details collected for $productIds")
      productsPrices  <- priceCalculator.finalPrices(user, products, preferences)
    } yield productsPrices

  private lazy val preferenceFetcher: PreferenceFetcher[F] =
    PreferenceFetcher(dep, logger)

  private lazy val productRepo: ProductRepo[F] =
    ProductRepo(dep, logger)

  private lazy val priceCalculator: PriceCalculator[F] =
    PriceCalculator[F](dep, logger)
}