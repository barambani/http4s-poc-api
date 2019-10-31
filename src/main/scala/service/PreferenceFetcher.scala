package service

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.PriceServiceError.InvalidShippingCountry
import integration.UserIntegration
import log.effect.LogWriter
import model.DomainModel._

sealed trait PreferenceFetcher[F[_]] {
  def userPreferences: UserId => F[UserPreferences]
}

object PreferenceFetcher {
  @inline def apply[F[_]](
    dep: UserIntegration[F],
    log: LogWriter[F]
  )(
    implicit ME: MonadError[F, Throwable]
  ): PreferenceFetcher[F] =
    new PreferenceFetcher[F] {
      def userPreferences: UserId => F[UserPreferences] =
        id =>
          for {
            pres <- dep.usersPreferences(id) <* log.debug(s"User preferences for $id collected successfully")
            valid <- log.debug(s"Validating user preferences for user $id") >>
                      validate(pres, id) <*
                      log.debug(s"User preferences for $id validated")
          } yield valid

      private def validate(p: UserPreferences, id: UserId): F[UserPreferences] =
        if (p.destination.country != "Italy") // Not very meaningful but it's to show the pattern
          log.error(s"InvalidShippingCountry: Cannot ship $id outside Italy") *>
            ME.raiseError[UserPreferences](
              InvalidShippingCountry("InvalidShippingCountry: Cannot ship outside Italy")
            )
        else p.pure[F]
    }
}
