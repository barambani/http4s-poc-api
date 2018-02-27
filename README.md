# Http4s Poc Api
[![Build Status](https://travis-ci.org/barambani/http4s-poc-api.svg?branch=master)](https://travis-ci.org/barambani/http4s-poc-api)
[![codecov](https://codecov.io/gh/barambani/http4s-poc-api/branch/master/graph/badge.svg)](https://codecov.io/gh/barambani/http4s-poc-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/barambani/http4s-poc-api/blob/master/LICENSE)

This repo contains a complete running example of a http api implemented with [http4s](http://http4s.org/) in tagless final Mtl style. It uses some helper tools from [Http4s Extend](https://github.com/barambani/http4s-extend).

### The Code
The logic is encoded into a tagless final DSL that abstracts over the concrete effectful computation.

```scala
/**
 * the DSL is encoded as a trait  
 */
sealed trait PreferenceFetcher[F[_]] {
  def userPreferences: UserId => F[UserPreferences]
}
```
where the capabilities of the `F[_]` are described with implicit evidences in Mtl style
```scala
@inline def apply[F[_] : MonadError[?[_], Throwable]](
  dependencies: Dependencies[F],
  logger: Logger[F]): PreferenceFetcher[F] =
    new PreferenceFetcherImpl(dependencies, logger)

private final class PreferenceFetcherImpl[F[_]](
  dependencies: Dependencies[F],
  logger      : Logger[F])(
    implicit
      F: MonadError[F, Throwable]) extends PreferenceFetcher[F] {

  def userPreferences: UserId => F[UserPreferences] =
    id => for {
      pres  <- dependencies.usersPreferences(id) <* logger.debug(s"User preferences for $id collected successfully")
      valid <- logger.debug(s"Validating user preferences for user $id") *> validate(pres, id) <* logger.debug(s"User preferences for $id collected successfully")
    } yield valid
    
  private def validate(p: UserPreferences, id: UserId): F[UserPreferences] = [...]
}
```
even where the need of running some steps in parallel exists, it's possible to express the capability in term of implicit evidences (`Parallel`)
```scala
final case class PriceService[F[_] : MonadError[?[_], Throwable] : Parallel[?[_], G], G[_]](
  dep: Dependencies[F], 
  logger: Logger[F]) {

  def prices(userId: UserId, productIds: Seq[ProductId]): F[List[Price]] =
    (userFor(userId), productsFor(productIds), preferencesFor(userId)).parMapN(finalPrices).flatten
    
  def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] = [...]
}
```
