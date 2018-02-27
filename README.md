# Http4s Poc Api
[![Build Status](https://travis-ci.org/barambani/http4s-poc-api.svg?branch=master)](https://travis-ci.org/barambani/http4s-poc-api)
[![codecov](https://codecov.io/gh/barambani/http4s-poc-api/branch/master/graph/badge.svg)](https://codecov.io/gh/barambani/http4s-poc-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/barambani/http4s-poc-api/blob/master/LICENSE)

This repo contains a complete running example of a http api implemented with [http4s](http://http4s.org/) in tagless final Mtl style. It uses some helper tools from [Http4s Extend](https://github.com/barambani/http4s-extend).

### The Code
The logic is encoded into a tagless final DSL that abstracts over the concrete effectful computation.

```scala
/**
 * the DSL is encoded as a set of traits
 */
sealed trait PreferenceFetcher[F[_]] {
  def userPreferences: UserId => F[UserPreferences]
}
```
where the capabilities of `F[_]` are provided trhough implicit evidences in Mtl style
```scala
@inline def apply[F[_] : MonadError[?[_], Throwable]](
  dependencies: Dependencies[F], logger: Logger[F]): PreferenceFetcher[F] =
    new PreferenceFetcherImpl(dependencies, logger)

private final class PreferenceFetcherImpl[F[_]](
  dependencies: Dependencies[F], logger: Logger[F])(
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
even where the need of running some steps in parallel exists, it's possible to express the capability the same way (`Parallel`)
```scala
final case class PriceService[F[_] : MonadError[?[_], Throwable] : Parallel[?[_], G], G[_]](
  dep: Dependencies[F], logger: Logger[F]) {

  def prices(userId: UserId, productIds: Seq[ProductId]): F[List[Price]] =
    (userFor(userId), productsFor(productIds), preferencesFor(userId)).parMapN(finalPrices).flatten
    
  def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] = [...]
}
```
The http api endpoint is implemented along the same idea and defines the capabilities of `F[_]` through type classes. In particular providing also implicit evidence for the `Decoder` of the payload and the `Encoder` for the response body makes clearly evident what are all the needs of the implementation, leaving very little to guessing
```scala
sealed abstract class PriceHttpApi[F[_], G[_]](
  implicit
    ME: MonadError[F, Throwable],
    RD: EntityDecoder[F, PricesRequestPayload],
    RE: EntityEncoder[F, List[Price]],
    TS: Show[Throwable],
    TR: ErrorResponse[F, Throwable]) extends Http4sDsl[F] {

  def service(priceService: PriceService[F, G]): HttpService[F] =
    HttpService[F] {
      case req @ Method.POST -> Root => postResponse(req, priceService) handleErrorWith TR.responseFor
    }

  private def postResponse(request: Request[F], priceService: PriceService[F, G]): F[Response[F]] =
    for {
      payload <- request.as[PricesRequestPayload]
      prices  <- priceService.prices(payload.userId, payload.productIds)
      resp    <- Ok(prices)
    } yield resp
}
```
This approach allows to worry about the details of the runtime and of the decoding/encoding only in the `Main` server file, where we have to materialize all the instances required to satisfy the implicit evidences
```scala
object Main extends StreamApp[IO] {

  implicit val futureExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new ForkJoinPool())

  implicit val monixTaskScheduler: Scheduler =
    Scheduler.global

  /**
    * encoding / decoding
    */
  implicit val priceRequestPayloadDecoder: EntityDecoder[IO, PricesRequestPayload] =
    jsonOf[IO, PricesRequestPayload]

  implicit val priceResponsePayloadEncoder: EntityEncoder[IO, List[Price]] =
    jsonEncoderOf[IO, List[Price]]

  implicit val healthCheckResponsePayloadEncoder: EntityEncoder[IO, ServiceSignature] =
    jsonEncoderOf[IO, ServiceSignature]

  /**
    * server
    */
  def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    BlazeBuilder[IO]
      .mountService(HealthCheckHttpApi[IO].service(), "/pricing-api/health-check")
      .mountService(PriceHttpApi[IO, IO.Par].service(priceService), "/pricing-api/prices")
      .enableHttp2(true)
      .serve

  private lazy val priceService: PriceService[IO, IO.Par] =
    PriceService(Dependencies[IO], Logger[IO])
}
```
