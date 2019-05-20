package service

import cats.Monad
import cats.instances.list._
import cats.instances.option._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.traverse._
import external.library.ParallelEffect
import interpreters.{ Dependencies, Logger }
import model.DomainModel._

sealed trait ProductRepo[F[_]] {
  def storedProducts: Seq[ProductId] => F[List[Product]]
}

object ProductRepo {

  @inline def apply[F[_]: Monad: ParallelEffect](
    dependencies: Dependencies[F],
    logger: Logger[F]
  ): ProductRepo[F] =
    new ProductRepoImpl(dependencies, logger)

  final private class ProductRepoImpl[F[_]: Monad: ParallelEffect](dep: Dependencies[F], logger: Logger[F])
      extends ProductRepo[F] {

    /**
      * Tries to retrieve the products by ProductId from the cache, if results
      * in a miss it tries on the http product store.
      * It returns only the products existing so the result might contain less
      * elements than the input list. If a product is not in the cache but is
      * found in the http store it will be added to the cache.
      */
    def storedProducts: Seq[ProductId] => F[List[Product]] =
      _.toList parTraverse (id => (cacheMissFetch(id) compose dep.cachedProduct)(id)) map (_.flatten)

    private def cacheMissFetch: ProductId => F[Option[Product]] => F[Option[Product]] =
      id =>
        cacheResult =>
          for {
            mayBeCached <- cacheResult
            mayBeStored <- mayBeCached.fold(
                            productStoreFetch(id) <* logger.debug(
                              s"Product $id not found in cache, fetched from the product store repo"
                            )
                          )(_.some.pure[F] <* logger.debug(s"Product $id found in cache"))
          } yield mayBeStored

    private def productStoreFetch(id: ProductId): F[Option[Product]] =
      for {
        mayBeProd <- dep.product(id)
        _         <- (mayBeProd map storeInCache).sequence
      } yield mayBeProd

    private def storeInCache: Product => F[Unit] =
      prod =>
        logger.debug(s"Storing the product ${prod.id} to cache") *>
          dep.storeProductToCache(prod.id)(prod) <*
          logger.debug(s"Product ${prod.id} stored into the cache")
  }
}
