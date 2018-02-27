package external

import model.DomainModel._
import monix.eval.Task

sealed trait TeamTwoDbApi {
  def user: UserId => Task[User]
  def product: ProductId => Task[Option[Product]]
}

object TeamTwoDbApi {

  @inline def apply(): TeamTwoDbApi =
    new TeamTwoDbApi {

      def user: UserId => Task[User] = ???

      def product: ProductId => Task[Option[Product]] = ???
    }
}