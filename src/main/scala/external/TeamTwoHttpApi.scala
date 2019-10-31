package external

import cats.effect.IO
import model.DomainModel._

trait TeamTwoHttpApi {
  def user: UserId => IO[User]
  def product: ProductId => IO[Option[Product]]
}

object TeamTwoHttpApi {
  @inline def apply(): TeamTwoHttpApi =
    new TeamTwoHttpApi {
      def user: UserId => IO[User]                  = ???
      def product: ProductId => IO[Option[Product]] = ???
    }
}
