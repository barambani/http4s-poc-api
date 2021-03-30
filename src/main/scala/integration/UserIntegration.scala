package integration

import cats.effect.syntax.concurrent._
import cats.effect.{Concurrent, IO}
import cats.syntax.flatMap._
import errors.PriceServiceError.{PreferenceErr, UserErr}
import external._
import external.library.IoAdapt.-->
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import cats.effect.{ Spawn, Temporal }

sealed trait UserIntegration[F[_]] {
  def user: UserId => F[User]
  def usersPreferences: UserId => F[UserPreferences]
}

object UserIntegration {
  @inline def apply[F[_]: Concurrent: Temporal: IO --> *[_]: Future --> *[_]](
    userDep: TeamTwoHttpApi,
    preferencesDep: TeamOneHttpApi,
    t: FiniteDuration
  ): UserIntegration[F] =
    new UserIntegration[F] {
      def user: UserId => F[User] = { id =>
        Spawn[F].cede >> userDep.user(id).adaptedTo[F].timeout(t).narrowFailureTo[UserErr]
      }

      def usersPreferences: UserId => F[UserPreferences] = { id =>
        Spawn[F].cede >> preferencesDep.usersPreferences(id).adaptedTo[F].timeout(t).narrowFailureTo[PreferenceErr]
      }
    }
}
