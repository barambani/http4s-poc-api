package service

import cats.instances.list._
import cats.instances.option._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.parallel._
import cats.syntax.traverse._
import cats.{ Monad, Parallel }
import integration.{ CacheIntegration, ProductIntegration }
import log.effect.LogWriter
import model.DomainModel._

sealed trait ProductRepo[F[_]] {
  def storedProducts: Seq[ProductId] => F[List[Product]]
}

object ProductRepo {

  @inline def apply[F[_]: Monad: Parallel[?[_]]](
    cache: CacheIntegration[F],
    productInt: ProductIntegration[F],
    logger: LogWriter[F]
  ): ProductRepo[F] =
    new ProductRepo[F] {

      /**
        * Tries to retrieve the products by ProductId from the cache, if results
        * in a miss it tries on the http product store.
        * It returns only the products existing so the result might contain less
        * elements than the input list. If a product is not in the cache but is
        * found in the http store it will be added to the cache.
        */
      def storedProducts: Seq[ProductId] => F[List[Product]] =
        _.toList parTraverse fromCacheOrStore map (_.flatten)

      private[this] def fromCacheOrStore: ProductId => F[Option[Product]] =
        id => cache.cachedProduct(id) >>= cacheMissFetch(id)

      private[this] def cacheMissFetch: ProductId => Option[Product] => F[Option[Product]] =
        id =>
          cacheResult =>
            for {
              mayBeStored <- cacheResult.fold(
                              productStoreFetch(id) <*
                                logger.debug(
                                  s"Product $id not found in cache, fetched from the product store repo"
                                )
                            )(_.some.pure[F] <* logger.debug(s"Product $id found in cache"))
            } yield mayBeStored

      private[this] def productStoreFetch(id: ProductId): F[Option[Product]] =
        for {
          mayBeProd <- productInt.product(id)
          _         <- (mayBeProd map storeInCache).sequence
        } yield mayBeProd

      private[this] def storeInCache: Product => F[Unit] =
        prod =>
          logger.debug(s"Storing the product ${prod.id} to cache") >>
            cache.storeProductToCache(prod.id)(prod) <*
            logger.debug(s"Product ${prod.id} stored into the cache")
    }
}
