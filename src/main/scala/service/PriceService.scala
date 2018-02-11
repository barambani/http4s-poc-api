package service

import cats.MonadError
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.ApiError
import interpreters.{Dependencies, Logger}
import model.DomainModel._

final case class PriceService[F[_] : MonadError[?[_], ApiError]](dep: Dependencies[F], logger: Logger[F]) {

  def prices(userId: UserId, productIds: Seq[ProductId]): F[Seq[Price]] =
    for {
      user            <- logger.debug(s"Collecting user details for id $userId") *> dep.user(userId)
      _               <- logger.debug(s"User details collected for id $userId")
      preferences     <- logger.debug(s"Looking up user preferences for user $userId") *> preferenceFetcher.userPreferences(userId)
      _               <- logger.debug(s"User preferences look up for $userId completed")
      products        <- logger.debug(s"Collecting product details for products $productIds") *> productRepo.storedProducts(productIds)
      _               <- logger.debug(s"Product details collection for $productIds completed")
      productsPrices  <- priceCalculator.finalPrices(user, products, preferences)
    } yield productsPrices

  private lazy val preferenceFetcher: PreferenceFetcher[F] =
    PreferenceFetcher(dep, logger)

  private lazy val productRepo: ProductRepo[F] =
    ProductRepo(dep, logger)

  private lazy val priceCalculator: PriceCalculator[F] =
    PriceCalculator[F](dep, logger)
}