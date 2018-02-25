package service

import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.parallel._
import cats.{MonadError, Parallel}
import interpreters.{Dependencies, Logger}
import model.DomainModel._

final case class PriceService[F[_] : MonadError[?[_], Throwable] : Parallel[?[_], G], G[_]](dep: Dependencies[F], logger: Logger[F]) {

  def prices(userId: UserId, productIds: Seq[ProductId]): F[List[Price]] =
    (userFor(userId), productsFor(productIds), preferencesFor(userId)).parMapN(priceCalculator.finalPrices).flatten

  def aaaaa[A, B, C](fa: F[A], fb: F[B], fc: F[C]): F[(A, B, C)] =
    Parallel.parMap3(fa, fb, fc)(Tuple3.apply)

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