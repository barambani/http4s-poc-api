package interpreters

import cats.effect.{ ConcurrentEffect, IO, Timer }
import cats.syntax.flatMap._
import external.TeamTwoHttpApi
import log.effect.LogWriter
import model.DomainModel.{ Product, ProductId, User, UserId }
import zio.clock.Clock
import zio.interop.catz._
import zio.{ Runtime, Task }

import scala.concurrent.duration._

object TestTeamTwoHttpApi {

  @inline def make(aUser: User, productsInStore: Map[ProductId, Product])(testLogger: LogWriter[Task])(
    implicit
    t: Timer[IO],
    rt: Runtime[Clock]
  ): TeamTwoHttpApi =
    new TeamTwoHttpApi {

      def user: UserId => IO[User] = { id =>
        ConcurrentEffect[Task].toIO(
          testLogger.debug(s"DEP user -> Getting the user $id in test")
        ) >> t.sleep(1.second) >> IO.delay(aUser)
      }

      def product: ProductId => IO[Option[Product]] = { id =>
        ConcurrentEffect[Task].toIO(
          testLogger.debug(s"DEP product -> Getting the product $id from the store in test")
        ) >> t.sleep(1.second) >> IO(productsInStore.get(id))
      }
    }

  @inline def makeFail(implicit t: Timer[IO]): TeamTwoHttpApi =
    new TeamTwoHttpApi {

      def user: UserId => IO[User] = { _ =>
        t.sleep(200.milliseconds) >> IO.delay(
          throw new Throwable(
            "DependencyFailure. The dependency `UserId => IO[User]` failed with message network failure"
          )
        )
      }

      def product: ProductId => IO[Option[Product]] = { _ =>
        t.sleep(400.milliseconds) >> IO.delay(
          throw new Throwable(
            "DependencyFailure. The dependency `ProductId => IO[Option[Product]]` failed with message network failure"
          )
        )
      }
    }
}
