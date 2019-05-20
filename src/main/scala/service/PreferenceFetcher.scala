package service

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.{ InvalidShippingCountry, ServiceError }
import interpreters.{ Dependencies, Logger }
import model.DomainModel._

sealed trait PreferenceFetcher[F[_]] {
  def userPreferences: UserId => F[UserPreferences]
}

object PreferenceFetcher {

  @inline def apply[F[_]: MonadError[?[_], ServiceError]](
    dependencies: Dependencies[F],
    logger: Logger[F]
  ): PreferenceFetcher[F] =
    new PreferenceFetcherImpl(dependencies, logger)

  final private class PreferenceFetcherImpl[F[_]](dependencies: Dependencies[F], logger: Logger[F])(
    implicit
    F: MonadError[F, ServiceError]
  ) extends PreferenceFetcher[F] {

    def userPreferences: UserId => F[UserPreferences] =
      id =>
        for {
          pres <- dependencies.usersPreferences(id) <* logger.debug(
                   s"User preferences for $id collected successfully"
                 )
          valid <- logger.debug(s"Validating user preferences for user $id") *> validate(pres, id) <* logger
                    .debug(s"User preferences for $id validated")
        } yield valid

    private def validate(p: UserPreferences, id: UserId): F[UserPreferences] =
      if (p.destination.country != "Italy") // Not very meaningful but it's to show the pattern
        logger.error(s"InvalidShippingCountry: Cannot ship $id outside Italy") *>
          F.raiseError[UserPreferences](
            InvalidShippingCountry(new Throwable("InvalidShippingCountry: Cannot ship outside Italy")).asServiceError
          )
      else p.pure[F]
  }
}
