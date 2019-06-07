package interpreters

import cats.effect.{ ContextShift, IO, Timer }
import cats.syntax.flatMap._
import integration.UserIntegration
import log.effect.LogWriter
import model.DomainModel.{ User, UserId, UserPreferences }

import scala.concurrent.duration._

object TestUserIntegration {

  @inline def make(aUser: User, preferences: UserPreferences)(testLogger: LogWriter[IO])(
    implicit
    ev1: Timer[IO],
    ev2: ContextShift[IO]
  ): UserIntegration[IO] =
    new UserIntegration[IO] {

      def user: UserId => IO[User] = { _ =>
        IO.shift >>
          testLogger.debug("DEP user -> Getting the user in test") >>
          IO.sleep(1.second) >>
          IO(aUser)
      }

      def usersPreferences: UserId => IO[UserPreferences] = { _ =>
        IO.shift >>
          testLogger.debug("DEP usersPreferences -> Getting the preferences in test") >>
          IO.sleep(1.second) >>
          IO(preferences)
      }
    }
}
