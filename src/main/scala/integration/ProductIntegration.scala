package integration

import cats.effect.syntax.concurrent._
import cats.effect.{ Concurrent, ContextShift, IO, Timer }
import cats.syntax.flatMap._
import errors.PriceServiceError.{ ProductErr, ProductPriceErr }
import external._
import external.library.IoAdapt.-->
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

sealed trait ProductIntegration[F[_]] {
  def product: ProductId => F[Option[Product]]
  def productPrice: Product => UserPreferences => F[Price]
}

object ProductIntegration {

  @inline def apply[F[_]: Concurrent: Timer: -->[IO, *[_]]: -->[Future, *[_]]](
    productDep: TeamTwoHttpApi,
    pricesDep: TeamOneHttpApi,
    t: FiniteDuration
  )(
    implicit CS: ContextShift[F]
  ): ProductIntegration[F] =
    new ProductIntegration[F] {

      def product: ProductId => F[Option[Product]] = { ps =>
        CS.shift >> productDep.product(ps).as[F].timeout(t).narrowFailureTo[ProductErr]
      }

      def productPrice: Product => UserPreferences => F[Price] = { p => pref =>
        CS.shift >> pricesDep.productPrice(p)(pref).as[F].timeout(t).narrowFailureTo[ProductPriceErr]
      }
    }
}
