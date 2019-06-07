package interpreters

import cats.effect.{ ContextShift, IO, Timer }
import cats.syntax.flatMap._
import integration.ProductIntegration
import log.effect.LogWriter
import model.DomainModel.{ Price, Product, ProductId, UserPreferences }

import scala.concurrent.duration._

object TestProductIntegration {

  @inline def make(productsInStore: Map[ProductId, Product], price: Price)(testLogger: LogWriter[IO])(
    implicit
    ev1: Timer[IO],
    ev2: ContextShift[IO]
  ): ProductIntegration[IO] =
    new ProductIntegration[IO] {

      def product: ProductId => IO[Option[Product]] = { id =>
        IO.shift >>
          testLogger.debug(s"DEP product -> Getting the product $id from the repo in test") >>
          IO.sleep(1.second) >>
          IO(productsInStore.get(id))
      }

      def productPrice: Product => UserPreferences => IO[Price] = { p => _ =>
        IO.shift >>
          testLogger.debug(s"DEP productPrice -> Getting the price for ${p.id} in test") >>
          IO.sleep(1.second) >>
          IO(price)
      }
    }

  @inline def makeFail(implicit ev: Timer[IO]): ProductIntegration[IO] =
    new ProductIntegration[IO] {

      def product: ProductId => IO[Option[Product]] = { _ =>
        IO.sleep(400.milliseconds) >>
          IO(
            throw new Throwable(
              "DependencyFailure. The dependency def product: ProductId => IO[Option[Product]] failed with message network failure"
            )
          )
      }

      def productPrice: Product => UserPreferences => IO[Price] = { _ => _ =>
        IO.sleep(600.milliseconds) >>
          IO(
            throw new Throwable(
              "DependencyFailure. The dependency def productPrice: Product => UserPreferences => IO[Price] failed with message timeout"
            )
          )
      }
    }
}
