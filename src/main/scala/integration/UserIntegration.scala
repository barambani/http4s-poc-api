package integration

import cats.effect.{ ContextShift, Sync }
import cats.syntax.flatMap._
import errors.PriceServiceError.{ PreferenceErr, UserErr }
import external._
import external.library.IoAdapt.-->
import external.library.ThrowableMap
import external.library.syntax.errorAdapt._
import external.library.syntax.ioAdapt._
import model.DomainModel._
import monix.eval.Task

import scala.concurrent.Future

sealed trait UserIntegration[F[_]] {
  def user: UserId => F[User]
  def usersPreferences: UserId => F[UserPreferences]
}

object UserIntegration {

  @inline def apply[F[_]: Sync: -->[Task, ?[_]]: -->[Future, ?[_]]](
    implicit
    CS: ContextShift[F],
    UE: ThrowableMap[UserErr],
    PE: ThrowableMap[PreferenceErr]
  ): UserIntegration[F] =
    new UserIntegration[F] {

      def user: UserId => F[User] = { id =>
        CS.shift >> TeamTwoHttpApi().user(id).as[F].narrowFailure(UE.map)
      }

      def usersPreferences: UserId => F[UserPreferences] = { id =>
        CS.shift >> TeamOneHttpApi().usersPreferences(id).as[F].narrowFailure(PE.map)
      }
    }
}
