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
          testLogger.debug("DEP product -> Getting the product from the repo in test") >>
          IO.sleep(1.second) >>
          IO(productsInStore.get(id))
      }

      def productPrice: Product => UserPreferences => IO[Price] = { _ => _ =>
        IO.shift >>
          testLogger.debug("DEP productPrice -> Getting the price in test") >>
          IO.sleep(1.second) >>
          IO(price)
      }
    }
}
