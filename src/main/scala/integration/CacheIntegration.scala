package integration

import cats.effect.syntax.concurrent._
import cats.effect.{Concurrent, IO}
import cats.syntax.flatMap._
import errors.PriceServiceError.{CacheLookupError, CacheStoreError}
import external.TeamThreeCacheApi
import external.library.IoAdapt.-->
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._

import scala.concurrent.duration.FiniteDuration
import cats.effect.{ Spawn, Temporal }

sealed trait CacheIntegration[F[_]] {
  def cachedProduct: ProductId => F[Option[Product]]
  def storeProductToCache: ProductId => Product => F[Unit]
}

object CacheIntegration {
  @inline def apply[F[_]: Concurrent: Temporal: IO --> *[_]](
    cache: TeamThreeCacheApi[ProductId, Product],
    t: FiniteDuration
  ): CacheIntegration[F] =
    new CacheIntegration[F] {
      def cachedProduct: ProductId => F[Option[Product]] =
        pId => Spawn[F].cede >> cache.get(pId).adaptedTo[F].timeout(t).narrowFailureTo[CacheLookupError]

      def storeProductToCache: ProductId => Product => F[Unit] =
        pId => p => Spawn[F].cede >> cache.put(pId)(p).adaptedTo[F].timeout(t).narrowFailureTo[CacheStoreError]
    }
}
