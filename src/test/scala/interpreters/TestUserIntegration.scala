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

      def user: UserId => IO[User] = { id =>
        IO.shift >>
          testLogger.debug(s"DEP user -> Getting the user $id in test") >>
          IO.sleep(1.second) >>
          IO(aUser)
      }

      def usersPreferences: UserId => IO[UserPreferences] = { id =>
        IO.shift >>
          testLogger.debug(s"DEP usersPreferences -> Getting the preferences for user $id in test") >>
          IO.sleep(1.second) >>
          IO(preferences)
      }
    }

  @inline def makeFail(implicit ev: Timer[IO]): UserIntegration[IO] =
    new UserIntegration[IO] {

      def user: UserId => IO[User] = { _ =>
        IO.sleep(200.milliseconds) >>
          IO(
            throw new Throwable(
              "DependencyFailure. The dependency def user: UserId => IO[User] failed with message network failure"
            )
          )
      }

      def usersPreferences: UserId => IO[UserPreferences] = { _ =>
        IO.sleep(100.milliseconds) >>
          IO(
            throw new Throwable(
              "DependencyFailure. The dependency def usersPreferences: UserId => IO[UserPreferences] failed with message timeout"
            )
          )
      }
    }
}
