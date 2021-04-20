package integration

import cats.effect.syntax.concurrent._
import cats.effect.{Concurrent, IO}
import cats.syntax.flatMap._
import errors.PriceServiceError.{ProductErr, ProductPriceErr}
import external._
import external.library.IoAdapt.-->
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import cats.effect.{ Spawn, Temporal }

sealed trait ProductIntegration[F[_]] {
  def product: ProductId => F[Option[Product]]
  def productPrice: Product => UserPreferences => F[Price]
}

object ProductIntegration {
  @inline def apply[F[_]: Concurrent: Temporal: IO --> *[_]: Future --> *[_]](
    productDep: TeamTwoHttpApi,
    pricesDep: TeamOneHttpApi,
    t: FiniteDuration
  ): ProductIntegration[F] =
    new ProductIntegration[F] {
      def product: ProductId => F[Option[Product]] = { ps =>
        Spawn[F].cede >> productDep.product(ps).adaptedTo[F].timeout(t).narrowFailureTo[ProductErr]
      }

      def productPrice: Product => UserPreferences => F[Price] = { p => pref =>
        Spawn[F].cede >> pricesDep.productPrice(p)(pref).adaptedTo[F].timeout(t).narrowFailureTo[ProductPriceErr]
      }
    }
}
