package interpreters

import external.TeamOneHttpApi
import model.DomainModel.{ Price, Product, UserId, UserPreferences }
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }

object TestTeamOneHttpApi {

  private val logger = LoggerFactory.getLogger("TestTeamOneHttpApi logger")

  @inline def make(preferences: UserPreferences, price: Price)(
    implicit ec: ExecutionContext
  ): TeamOneHttpApi =
    new TeamOneHttpApi {

      def usersPreferences: UserId => Future[UserPreferences] = { id =>
        Future {
          logger.debug(s"DEP usersPreferences -> Getting the preferences for user $id in test")
          Thread.sleep(1000)
          logger.debug(s"DEP usersPreferences -> Preferences for user $id returned in test")
          preferences
        }
      }

      def productPrice: Product => UserPreferences => Future[Price] = { p => _ =>
        Future {
          logger.debug(s"DEP productPrice -> Getting the price for product ${p.id} in test")
          Thread.sleep(1000)
          logger.debug(s"DEP usersPreferences -> Price for product ${p.id} returned in test")
          price
        }
      }
    }

  @inline def makeFail(implicit ec: ExecutionContext): TeamOneHttpApi =
    new TeamOneHttpApi {

      def usersPreferences: UserId => Future[UserPreferences] = { id =>
        Future {
          logger.debug(s"DEP usersPreferences -> Starting a failing call to get preferences for $id in test")
          Thread.sleep(100)
          throw new Throwable(
            "DependencyFailure. The dependency `UserId => Future[UserPreferences]` failed with message timeout"
          )
        }
      }

      def productPrice: Product => UserPreferences => Future[Price] = { p => _ =>
        Future {
          logger.debug(
            s"DEP productPrice -> Starting a failing call to get the price for product ${p.id} in test"
          )
          Thread.sleep(100)
          throw new Throwable(
            "DependencyFailure. The dependency `Product => UserPreferences => Future[Price]` failed with message timeout"
          )
        }
      }
    }
}
