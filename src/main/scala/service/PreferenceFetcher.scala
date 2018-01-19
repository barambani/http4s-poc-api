package service

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.{ApiError, InvalidShippingCountry}
import model.DomainModel._

sealed trait PreferenceFetcher[F[_]] {
  def userPreferences: UserId => F[UserPreferences]
}

final case class PreferenceFetcherImpl[F[_]](
  dependencies: Dependencies[F],
  logger      : Logger[F])(
    implicit
      F: MonadError[F, ApiError]) extends PreferenceFetcher[F] {

  def userPreferences: UserId => F[UserPreferences] =
    id => for {
      pres  <- dependencies.usersPreferences(id)  <* logger.info(s"Preferences for $id collected")
      valid <- validate(pres)                     <* logger.info(s"Preferences for $id validated")
    } yield valid

  private def validate(p: UserPreferences): F[UserPreferences] =
    if(p.destination.country != "Italy") // No very meaningful but it's to show the pattern
      F.raiseError[UserPreferences](InvalidShippingCountry("Cannot ship outside Italy")) <* logger.error(s"InvalidShippingCountry Cannot ship outside Italy")
    else
      p.pure[F]
}