package dependencies

import model.DomainModel._

import scala.concurrent.Future

sealed trait TeamOneHttpApi {
  def usersPreferences: UserId => Future[UserPreferences]
  def productPrice: Product => UserPreferences => Future[Price]
}

object DummyTeamOneHttpApi extends TeamOneHttpApi {
  def usersPreferences: UserId => Future[UserPreferences] = ???
  def productPrice: Product => UserPreferences => Future[Price] = ???
}