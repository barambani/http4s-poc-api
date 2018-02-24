package external

import model.DomainModel._
import monix.eval.Task

sealed trait TeamTwoHttpApi {
  def user: UserId => Task[User]
  def product: ProductId => Task[Option[Product]]
}

object TeamTwoHttpApi {

  @inline def apply(): TeamTwoHttpApi = new DummyTeamTwoHttpApi

  private final class DummyTeamTwoHttpApi extends TeamTwoHttpApi {

    def user: UserId => Task[User] = ???

    def product: ProductId => Task[Option[Product]] = ???
  }
}