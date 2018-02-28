package service

import cats.MonadError
import cats.syntax.apply._
import cats.syntax.flatMap._
import http4s.extend.ParEffectful
import http4s.extend.syntax.parEffectful._
import interpreters.{Dependencies, Logger}
import model.DomainModel._

final case class PriceService[F[_] : MonadError[?[_], Throwable] : ParEffectful](dep: Dependencies[F], logger: Logger[F]) {

  def prices(userId: UserId, productIds: Seq[ProductId]): F[List[Price]] =
    (userFor(userId), productsFor(productIds), preferencesFor(userId)).parMap(priceCalculator.finalPrices).flatten
  

  private def userFor(userId: UserId): F[User] =
    logger.debug(s"Collecting user details for id $userId") *> dep.user(userId) <* logger.debug(s"User details collected for id $userId")

  private def preferencesFor(userId: UserId): F[UserPreferences] =
    logger.debug(s"Looking up user preferences for user $userId") *> preferenceFetcher.userPreferences(userId) <* logger.debug(s"User preferences look up for $userId completed")

  private def productsFor(productIds: Seq[ProductId]): F[List[Product]] =
    logger.debug(s"Collecting product details for products $productIds") *> productRepo.storedProducts(productIds) <* logger.debug(s"Product details collection for $productIds completed")


  private lazy val preferenceFetcher: PreferenceFetcher[F] =
    PreferenceFetcher(dep, logger)

  private lazy val productRepo: ProductRepo[F] =
    ProductRepo(dep, logger)

  private lazy val priceCalculator: PriceCalculator[F] =
    PriceCalculator[F](dep, logger)
}