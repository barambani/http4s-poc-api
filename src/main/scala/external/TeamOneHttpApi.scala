package external

import model.DomainModel._

import scala.concurrent.Future

sealed trait TeamOneHttpApi {
  def usersPreferences: UserId => Future[UserPreferences]
  def productPrice: Product => UserPreferences => Future[Price]
}

object TeamOneHttpApi {

  @inline def apply(): TeamOneHttpApi = new DummyTeamOneHttpApi

  private final class DummyTeamOneHttpApi extends TeamOneHttpApi {

    def usersPreferences: UserId => Future[UserPreferences] = ???

    def productPrice: Product => UserPreferences => Future[Price] = ???
  }
}