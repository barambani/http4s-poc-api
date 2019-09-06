package integration

import cats.effect.syntax.concurrent._
import cats.effect.{ Concurrent, ContextShift, IO, Timer }
import cats.syntax.flatMap._
import errors.PriceServiceError.{ CacheLookupError, CacheStoreError }
import external.TeamThreeCacheApi
import external.library.IoAdapt.-->
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._

import scala.concurrent.duration.FiniteDuration

sealed trait CacheIntegration[F[_]] {
  def cachedProduct: ProductId => F[Option[Product]]
  def storeProductToCache: ProductId => Product => F[Unit]
}

object CacheIntegration {

  @inline def apply[F[_]: Concurrent: Timer: -->[IO, *[_]]](
    cache: TeamThreeCacheApi[ProductId, Product],
    t: FiniteDuration
  )(
    implicit CS: ContextShift[F]
  ): CacheIntegration[F] =
    new CacheIntegration[F] {

      def cachedProduct: ProductId => F[Option[Product]] =
        pId => CS.shift >> cache.get(pId).as[F].timeout(t).narrowFailureTo[CacheLookupError]

      def storeProductToCache: ProductId => Product => F[Unit] =
        pId => p => CS.shift >> cache.put(pId)(p).as[F].timeout(t).narrowFailureTo[CacheStoreError]
    }
}
