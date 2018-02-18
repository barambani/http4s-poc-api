package service

import cats.MonadError
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.ApiError
import http4s.extend.ParEffectful
import interpreters.{Dependencies, Logger}
import model.DomainModel._

final case class PriceService[F[_] : MonadError[?[_], ApiError]](dep: Dependencies[F], logger: Logger[F])(implicit ev: ParEffectful[F]) {

  def prices(userId: UserId, productIds: Seq[ProductId]): F[Seq[Price]] =
    for {
      retrievalResult               <- ev.parTupled3(userFor(userId), productsFor(productIds), preferencesFor(userId))
      (user, products, preferences) =  retrievalResult
      productsPrices                <- priceCalculator.finalPrices(user, products, preferences)
    } yield productsPrices

  private def userFor(userId: UserId): F[User] =
    for {
      user <- logger.debug(s"Collecting user details for id $userId") *> dep.user(userId)
      _    <- logger.debug(s"User details collected for id $userId")
    } yield user

  private def preferencesFor(userId: UserId): F[UserPreferences] =
    for {
      preferences <- logger.debug(s"Looking up user preferences for user $userId") *> preferenceFetcher.userPreferences(userId)
      _           <- logger.debug(s"User preferences look up for $userId completed")
    } yield preferences

  private def productsFor(productIds: Seq[ProductId]): F[List[Product]] =
    for {
      products <- logger.debug(s"Collecting product details for products $productIds") *> productRepo.storedProducts(productIds)
      _        <- logger.debug(s"Product details collection for $productIds completed")
    } yield products


  private lazy val preferenceFetcher: PreferenceFetcher[F] =
    PreferenceFetcher(dep, logger)

  private lazy val productRepo: ProductRepo[F] =
    ProductRepo(dep, logger)

  private lazy val priceCalculator: PriceCalculator[F] =
    PriceCalculator[F](dep, logger)
}