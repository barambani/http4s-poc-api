package service

import cats.MonadError
import cats.syntax.apply._
import cats.syntax.flatMap._
import http4s.extend.ParEffectful
import interpreters.{Dependencies, Logger}
import model.DomainModel._

final case class PriceService[F[_] : MonadError[?[_], Throwable] : ParEffectful](dep: Dependencies[F], logger: Logger[F]) {

  def prices(userId: UserId, productIds: Seq[ProductId]): F[List[Price]] =
    (userFor(userId), productsFor(productIds), preferencesFor(userId)).parMap3(priceCalculator.finalPrices).flatten
  

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

  implicit final class Syntax2[FF[_], A, B](t2: (FF[A], FF[B]))(implicit ev: ParEffectful[FF]) {

    def parMap2[R](f: (A, B) => R): FF[R] =
      ev.parMap2(t2._1, t2._2)(f)

    def parTupled2: FF[(A, B)] =
      ev.parTupled2(t2._1, t2._2)
  }

  implicit final class Syntax3[FF[_], A, B, C](t3: (FF[A], FF[B], FF[C]))(implicit ev: ParEffectful[FF]) {

    def parMap3[R](f: (A, B, C) => R): FF[R] =
      ev.parMap3(t3._1, t3._2, t3._3)(f)

    def parTupled3: FF[(A, B, C)] =
      ev.parTupled3(t3._1, t3._2, t3._3)
  }
}