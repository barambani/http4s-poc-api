package integration

import cats.effect.{ ContextShift, Sync }
import cats.syntax.flatMap._
import errors.PriceServiceError.CacheLookupError
import external.TeamThreeCacheApi._
import external._
import external.library.IoAdapt.-->
import external.library.ThrowableMap
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._
import scalaz.concurrent.Task

trait CacheIntegration[F[_]] {
  def cachedProduct: ProductId => F[Option[Product]]
  def storeProductToCache: ProductId => Product => F[Unit]
}

object CacheIntegration {

  @inline def apply[F[_]: Sync: -->[Task, ?[_]]](
    implicit
    CE: ThrowableMap[CacheLookupError],
    CS: ContextShift[F]
  ): CacheIntegration[F] =
    new CacheIntegration[F] {

      def cachedProduct: ProductId => F[Option[Product]] =
        pId => CS.shift >> TeamThreeCacheApi[ProductId, Product].get(pId).as[F].narrowFailure(CE.map)

      def storeProductToCache: ProductId => Product => F[Unit] =
        pId => p => CS.shift >> TeamThreeCacheApi[ProductId, Product].put(pId)(p).as[F].narrowFailure(CE.map)
    }
}
