package interpreters

import cats.effect.{ConcurrentEffect, IO}
import cats.syntax.flatMap._
import external.TeamThreeCacheApi
import log.effect.LogWriter
import model.DomainModel.{Product, ProductId}
import zio.clock.Clock
import zio.interop.catz._
import zio.{Runtime, Task}

import scala.concurrent.duration._
import cats.effect.Temporal

object TestTeamThreeCacheApi {
  @inline def make(productsInCache: Map[ProductId, Product])(testLogger: LogWriter[Task])(
    implicit t: Temporal[IO],
    rt: Runtime[Clock]
  ): TeamThreeCacheApi[ProductId, Product] =
    new TeamThreeCacheApi[ProductId, Product] {
      def get: ProductId => IO[Option[Product]] = { id =>
        ConcurrentEffect[Task].toIO(
          testLogger.debug(s"DEP cachedProduct -> Getting the product $id from the cache in test")
        ) >> t.sleep(200.milliseconds) >> IO(productsInCache.get(id))
      }

      def put: ProductId => Product => IO[Unit] = { id => _ =>
        ConcurrentEffect[Task].toIO(
          testLogger.debug(s"DEP storeProductToCache -> Storing the product $id to the repo in test")
        ) >> t.sleep(200.milliseconds) >> IO.unit
      }
    }

  @inline def makeFail(implicit t: Temporal[IO]): TeamThreeCacheApi[ProductId, Product] =
    new TeamThreeCacheApi[ProductId, Product] {
      def get: ProductId => IO[Option[Product]] = { _ =>
        t.sleep(300.milliseconds) >> IO.delay(
          throw new Throwable(
            "DependencyFailure. The dependency def cachedProduct: ProductId => Task[Option[Product]] failed with message not responding"
          )
        )
      }

      def put: ProductId => Product => IO[Unit] = { _ => _ =>
        t.sleep(150.milliseconds) >> IO.delay(
          throw new Throwable(
            "DependencyFailure. The dependency def storeProductToCache: ProductId => Product => Task[Unit] failed with message not responding"
          )
        )
      }
    }
}
