package service

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import errors.{ApiError, InvalidShippingCountry}
import model.DomainModel._

sealed trait PreferenceFetcher[F[_]] {
  def fetchUserPreferences: UserId => F[UserPreferences]
}

final case class PreferenceFetcherImpl[F[_]](
  dependencies: Dependencies[F],
  logger      : Logger[F])(
    implicit
      F: MonadError[F, ApiError]) extends PreferenceFetcher[F] {

  def fetchUserPreferences: UserId => F[UserPreferences] =
    id => for {
      pres  <- dependencies.usersPreferences(id)  <* logger.info(s"Preferences for $id collected")
      valid <- validate(pres)                     <* logger.info(s"validated prefs for $id")
    } yield valid

  private def validate(p: UserPreferences): F[UserPreferences] =
    if(p.destination.country != "Italy")
      F.raiseError[UserPreferences](InvalidShippingCountry("Cannot ship outside Italy")) <* logger.error(s"InvalidShippingCountry Cannot ship outside Italy")
    else
      p.pure[F]
}