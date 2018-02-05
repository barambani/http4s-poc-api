package service

import cats.MonadError
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import errors.ApiError
import interpreters.{Dependencies, Logger}
import model.DomainModel._

import scala.language.higherKinds

sealed trait ProductRepo[F[_]] {
  def storedProducts: Seq[ProductId] => F[List[Product]]
}

object ProductRepo {

  @inline def apply[F[_] : MonadError[?[_], ApiError]](dependencies: Dependencies[F], logger: Logger[F]): ProductRepo[F] =
    new ProductRepoImpl(dependencies, logger)

  private final class ProductRepoImpl[F[_] : MonadError[?[_], ApiError]](dep : Dependencies[F], logger: Logger[F]) extends ProductRepo[F] {

    // TODO: Run in parallel in F
    def storedProducts: Seq[ProductId] => F[List[Product]] =
      _.toList.map { id => (cacheMissFetch(id) compose dep.cachedProduct)(id) }.sequence

    private def cacheMissFetch: ProductId => F[Option[Product]] => F[Product] =
      id => cacheResult => for {
        mayBeProduct <- cacheResult
        product      <- mayBeProduct.fold(httpFetch(id)){ _.pure[F] <* logger.info(s"Product $id found in cache") }
      } yield product

    private def httpFetch(id: ProductId): F[Product] =
      for {
        prod  <- dep.product(id)                        <* logger.info(s"Product $id not in cache, fetched from the repo")
        _     <- dep.storeProductToCache(prod.id)(prod) <* logger.info(s"Product $id stored to cache")
      } yield prod
  }
}