package interpreters

import cats.effect.{ ContextShift, IO, Timer }
import cats.syntax.flatMap._
import integration.CacheIntegration
import log.effect.LogWriter
import model.DomainModel.{ Product, ProductId }

import scala.concurrent.duration._

object TestCacheIntegration {

  @inline def make(productsInCache: Map[ProductId, Product])(testLogger: LogWriter[IO])(
    implicit
    ev1: Timer[IO],
    ev2: ContextShift[IO]
  ): CacheIntegration[IO] =
    new CacheIntegration[IO] {

      def cachedProduct: ProductId => IO[Option[Product]] = { id =>
        IO.shift >>
          testLogger.debug(s"DEP cachedProduct -> Getting the product $id from the cache in test") >>
          IO.sleep(200.milliseconds) >>
          IO(productsInCache.get(id))
      }

      def storeProductToCache: ProductId => Product => IO[Unit] = { id => _ =>
        IO.shift >> testLogger.debug(
          s"DEP storeProductToCache -> Storing the product $id to the repo in test"
        ) >>
          IO.sleep(200.milliseconds) >>
          IO.unit
      }
    }

  @inline def makeFail(implicit ev: Timer[IO]): CacheIntegration[IO] =
    new CacheIntegration[IO] {

      def cachedProduct: ProductId => IO[Option[Product]] = { _ =>
        IO.sleep(300.milliseconds) >>
          IO(
            throw new Throwable(
              "DependencyFailure. The dependency def cachedProduct: ProductId => IO[Option[Product]] failed with message not responding"
            )
          )
      }

      def storeProductToCache: ProductId => Product => IO[Unit] = { _ => _ =>
        IO.sleep(150.milliseconds) >>
          IO(
            throw new Throwable(
              "DependencyFailure. The dependency def storeProductToCache: ProductId => Product => IO[Unit] failed with message not responding"
            )
          )
      }
    }
}
