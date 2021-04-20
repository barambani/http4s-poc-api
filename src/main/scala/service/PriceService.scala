package service

import cats.Parallel
import cats.effect.{Concurrent, IO}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.parallel._
import external.library.IoAdapt.-->
import external.{TeamOneHttpApi, TeamThreeCacheApi, TeamTwoHttpApi}
import integration.{CacheIntegration, ProductIntegration, UserIntegration}
import log.effect.LogWriter
import model.DomainModel._

import scala.concurrent.Future
import scala.concurrent.duration._
import cats.effect.Temporal

final case class PriceService[F[_]: Concurrent: Temporal: ContextShift: Parallel[*[_]]](
  cacheDep: TeamThreeCacheApi[ProductId, Product],
  teamOneStupidName: TeamOneHttpApi,
  teamTwoStupidName: TeamTwoHttpApi,
  logger: LogWriter[F]
)(
  implicit ev1: IO --> F,
  ev2: Future --> F
) {
  private[this] val cache      = CacheIntegration[F](cacheDep, 10.seconds)
  private[this] val userInt    = UserIntegration[F](teamTwoStupidName, teamOneStupidName, 10.seconds)
  private[this] val productInt = ProductIntegration[F](teamTwoStupidName, teamOneStupidName, 10.seconds)

  private[this] lazy val productRepo: ProductRepo[F]             = ProductRepo(cache, productInt, logger)
  private[this] lazy val priceCalculator: PriceCalculator[F]     = PriceCalculator(productInt, logger)
  private[this] lazy val preferenceFetcher: PreferenceFetcher[F] = PreferenceFetcher(userInt, logger)

  /**
   * Going back to ParallelEffect and the fs2 implementation as the new cats.effect version 0.10 changes the semantic
   * of parMapN because of the cancellation. It is not able anymore to collect multiple errors in the resulting
   * MonadError as explained in this gitter conversation
   *
   * https://gitter.im/typelevel/cats-effect*at=5aac5013458cbde55742ef7e
   *
   * While waiting for a different solution with cats.Parallel, this suits the purpose better
   */
  def prices(userId: UserId, productIds: Seq[ProductId]): F[List[Price]] =
    (userFor(userId), productsFor(productIds), preferencesFor(userId))
      .parMapN(priceCalculator.finalPrices)
      .flatten

  private[this] def userFor(userId: UserId): F[User] =
    logger.debug(s"Collecting user details for id $userId") >>
      userInt.user(userId) <*
      logger.debug(s"User details collected for id $userId")

  private[this] def preferencesFor(userId: UserId): F[UserPreferences] =
    logger.debug(s"Looking up user preferences for user $userId") >>
      preferenceFetcher.userPreferences(userId) <*
      logger.debug(s"User preferences look up for $userId completed")

  private[this] def productsFor(productIds: Seq[ProductId]): F[List[Product]] =
    logger.debug(s"Collecting product details for products $productIds") >>
      productRepo.storedProducts(productIds) <*
      logger.debug(s"Product details collection for $productIds completed")
}
