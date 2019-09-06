package integration

import cats.effect.syntax.concurrent._
import cats.effect.{ Concurrent, ContextShift, IO, Timer }
import cats.syntax.flatMap._
import errors.PriceServiceError.{ PreferenceErr, UserErr }
import external._
import external.library.IoAdapt.-->
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

sealed trait UserIntegration[F[_]] {
  def user: UserId => F[User]
  def usersPreferences: UserId => F[UserPreferences]
}

object UserIntegration {

  @inline def apply[F[_]: Concurrent: Timer: -->[IO, *[_]]: -->[Future, *[_]]](
    userDep: TeamTwoHttpApi,
    preferencesDep: TeamOneHttpApi,
    t: FiniteDuration
  )(
    implicit CS: ContextShift[F]
  ): UserIntegration[F] =
    new UserIntegration[F] {

      def user: UserId => F[User] = { id =>
        CS.shift >> userDep.user(id).as[F].timeout(t).narrowFailureTo[UserErr]
      }

      def usersPreferences: UserId => F[UserPreferences] = { id =>
        CS.shift >> preferencesDep.usersPreferences(id).as[F].timeout(t).narrowFailureTo[PreferenceErr]
      }
    }
}
