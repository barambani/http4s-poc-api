package service

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.InvalidShippingCountry
import interpreters.{Dependencies, Logger}
import model.DomainModel._

sealed trait PreferenceFetcher[F[_]] {
  def userPreferences: UserId => F[UserPreferences]
}

object PreferenceFetcher {

  @inline def apply[F[_] : MonadError[?[_], Throwable]](dependencies: Dependencies[F], logger: Logger[F]): PreferenceFetcher[F] =
    new PreferenceFetcherImpl(dependencies, logger)

  private final class PreferenceFetcherImpl[F[_]](
    dependencies: Dependencies[F],
    logger      : Logger[F])(
      implicit
        F: MonadError[F, Throwable]) extends PreferenceFetcher[F] {

    def userPreferences: UserId => F[UserPreferences] =
      id => for {
        pres  <- logger.debug(s"Collecting user preferences for user $id") *> dependencies.usersPreferences(id)
        _     <- logger.debug(s"User preferences for $id collected successfully")
        valid <- logger.debug(s"Validating user preferences for user $id") *> validate(pres)
        _     <- logger.debug(s"User preferences for $id collected successfully")
      } yield valid

    private def validate(p: UserPreferences): F[UserPreferences] =
      if(p.destination.country != "Italy") // Not very meaningful but it's to show the pattern
        F.raiseError[UserPreferences](InvalidShippingCountry("Cannot ship outside Italy")) <* logger.error(s"InvalidShippingCountry: Cannot ship outside Italy")
      else
        p.pure[F]
  }
}