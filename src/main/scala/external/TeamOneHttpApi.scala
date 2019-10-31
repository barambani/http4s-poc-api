package external

import model.DomainModel._

import scala.concurrent.Future

trait TeamOneHttpApi {
  def usersPreferences: UserId => Future[UserPreferences]
  def productPrice: Product => UserPreferences => Future[Price]
}

object TeamOneHttpApi {
  @inline def apply(): TeamOneHttpApi =
    new TeamOneHttpApi {
      def usersPreferences: UserId => Future[UserPreferences]       = ???
      def productPrice: Product => UserPreferences => Future[Price] = ???
    }
}
