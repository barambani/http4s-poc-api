package external

import model.DomainModel._
import monix.eval.Task

sealed trait TeamTwoHttpApi {
  def user: UserId => Task[User]
  def product: ProductId => Task[Product]
}

object DummyTeamTwoHttpApi extends TeamTwoHttpApi {
  def user: UserId => Task[User] = ???
  def product: ProductId => Task[Product] = ???
}