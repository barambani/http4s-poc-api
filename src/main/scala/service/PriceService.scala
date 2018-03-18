package service

import cats.{MonadError, Parallel}
import cats.syntax.apply._
import cats.syntax.flatMap._
import http4s.extend.ParEffectful
import http4s.extend.syntax.parEffectful._
import interpreters.{Dependencies, Logger}
import model.DomainModel._

final case class PriceService[F[_] : MonadError[?[_], Throwable] : ParEffectful, P[_]: Parallel[F, ?[_]]](
  dep: Dependencies[F], logger: Logger[F]) {

  /**
    * Going back to ParEffectful and the fs2 implementation as the new cats.effect version 0.10 changes the semantic
    * of parMapN because of the cancellation. It is not able anymore to collect multiple errors in the resulting
    * MonadError as explained in this gitter conversation
    *
    * https://gitter.im/typelevel/cats-effect?at=5aac5013458cbde55742ef7e
    *
    * While waiting for a different solution with cats.Parallel, this suits the purpose better
    */
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
    PriceCalculator(dep, logger)
}