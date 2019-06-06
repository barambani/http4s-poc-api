package integration

import cats.effect.{ ContextShift, Sync }
import cats.syntax.flatMap._
import errors.PriceServiceError.{ ProductErr, ProductPriceErr }
import external._
import external.library.IoAdapt.-->
import external.library.ThrowableMap
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._
import monix.eval.Task

import scala.concurrent.Future

sealed trait ProductIntegration[F[_]] {
  def product: ProductId => F[Option[Product]]
  def productPrice: Product => UserPreferences => F[Price]
}

object ProductIntegration {

  @inline def apply[F[_]: Sync: -->[Task, ?[_]]: -->[Future, ?[_]]](
    implicit
    CS: ContextShift[F],
    PE: ThrowableMap[ProductErr],
    PPE: ThrowableMap[ProductPriceErr]
  ): ProductIntegration[F] =
    new ProductIntegration[F] {

      def product: ProductId => F[Option[Product]] = { ps =>
        CS.shift >> TeamTwoHttpApi().product(ps).as[F].narrowFailure(PE.map)
      }

      def productPrice: Product => UserPreferences => F[Price] = { p => pref =>
        CS.shift >> TeamOneHttpApi().productPrice(p)(pref).as[F].narrowFailure(PPE.map)
      }
    }
}
