# Http4s Poc Api
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/117ccce325c04aada3d56a657aab170c)](https://app.codacy.com/app/barambani/http4s-poc-api?utm_source=github.com&utm_medium=referral&utm_content=barambani/http4s-poc-api&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.org/barambani/http4s-poc-api.svg?branch=master)](https://travis-ci.org/barambani/http4s-poc-api)
[![codecov](https://codecov.io/gh/barambani/http4s-poc-api/branch/master/graph/badge.svg)](https://codecov.io/gh/barambani/http4s-poc-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/barambani/http4s-poc-api/blob/master/LICENSE)

This repo contains a complete example of a http api implemented with [http4s](http://http4s.org/) in tagless final Mtl style. It uses some helper tools from [Http4s Extend](https://github.com/barambani/http4s-extend).

### Service Structure

#### Business Logic
The logic is encoded into a tagless final DSL that abstracts over the concrete effectful computation.

```scala
/**
 * the DSL is encoded as a set of traits
 */
sealed trait PreferenceFetcher[F[_]] {
  def userPreferences: UserId => F[UserPreferences]
}
```
where the capabilities of `F[_]` are provided trhough implicit evidences
```scala
@inline def apply[F[_] : MonadError[?[_], ServiceError]](
  dependencies: Dependencies[F], logger: Logger[F]): PreferenceFetcher[F] =
    new PreferenceFetcherImpl(dependencies, logger)

private final class PreferenceFetcherImpl[F[_]](
  dependencies: Dependencies[F],
  logger      : Logger[F])(
    implicit
      F: MonadError[F, ServiceError]) extends PreferenceFetcher[F] {

  def userPreferences: UserId => F[UserPreferences] =
    id => for {
      pres  <- dependencies.usersPreferences(id) <* logger.debug(s"User preferences for $id collected successfully")
      valid <- logger.debug(s"Validating user preferences for user $id") *> validate(pres, id) <* logger.debug(s"User preferences for $id validated")
    } yield valid

  private def validate(p: UserPreferences, id: UserId): F[UserPreferences] = [...]
}
```

#### Parallel Execution
When there's the need of running computations in parallel, the same approach is used. This capability is guaranteed by the evidence `F[_] : ParEffectful` that enables the usage of `parMap` ([see the lib here](https://github.com/barambani/http4s-extend/blob/master/src/main/scala/http4s/extend/ParEffectful.scala)).
```scala
final case class PriceService[F[_] : MonadError[?[_], ServiceError] : ParEffectful](
  dep: Dependencies[F], logger: Logger[F]) {
  
  def prices(userId: UserId, productIds: Seq[ProductId]): F[List[Price]] =
    (userFor(userId), productsFor(productIds), preferencesFor(userId)).parMap(finalPrices).flatten
    
  def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] = [...]
}
```
Sometimes is also usefull to fire some external dependencies in parallel when there is a collection of known cohordinates as source. In this repo for instance, this is the case when the service needs to check the cache for a list of `ProductId` and collect their details in case they exist. As before, this computation can be described again at a high level of abstraction using `ParEffectful` and its `parTraverse` function ([see the implementation here](https://github.com/barambani/http4s-extend/blob/master/src/main/scala/http4s/extend/ParEffectful.scala#L60)).
```scala
private final class ProductRepoImpl[F[_] : Monad : ParEffectful](
  dep : Dependencies[F], logger: Logger[F]) extends ProductRepo[F] {
  
  /**
    * Tries to retrieve the products by ProductId from the cache, if results in a miss it tries on the http store.
    * It returns only the products existing so the result might contain less elements than the input list.
    * If a product is not in the cache but is found in the http store it will be added to the cache
    */
  def storedProducts: Seq[ProductId] => F[List[Product]] =
    _.toList.parTraverse(id => (cacheMissFetch(id) compose dep.cachedProduct)(id)) map (_.flatten)

  private def cacheMissFetch: ProductId => F[Option[Product]] => F[Option[Product]] = [...]
}
```

#### Http Endpoints
The http api endpoint is implemented following the same style. The capabilities of `F[_]` are described through type classes. At the same level are also provided evidences for the `Decoder` of the request payload and for the `Encoder` of the response body. This describes well everything the `F[_]` will have to provide to make the endpoint implementation work.
```scala
sealed abstract class PriceHttpApi[F[_]](
  implicit
    ME: MonadError[F, ServiceError],
    RD: EntityDecoder[F, PricesRequestPayload],
    RE: EntityEncoder[F, List[Price]],
    ER: ErrorResponse[F, ServiceError]) extends Http4sDsl[F] {

  def service(priceService: PriceService[F]): HttpService[F] =
    HttpService[F] {
      case req @ Method.POST -> Root => postResponse(req, priceService) handleErrorWith ER.responseFor
    }

  private def postResponse(request: Request[F], priceService: PriceService[F]): F[Response[F]] =
    for {
      payload <- request.as[PricesRequestPayload]
      prices  <- priceService.prices(payload.userId, payload.productIds)
      resp    <- Ok(prices)
    } yield resp
}
```

#### Main Server
This approach decouples very well the details of the execution and the decoding/encoding from the domain logic's formalization. With this style is possible to describe at a very high level of abstraction the expected behavior of the system and the domain context at hand, tuning the power of the structures needed by any implementation in a particurarly fine way. The sole place where the actual runtime becomes relevant is in the `Main` server file where all the instances are materialized (notice all the occurrencies of IO below that don't appear in the interanls of the service).
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
      .mountService(PriceHttpApi[IO].service(priceService), "/pricing-api/prices")
      .enableHttp2(true)
      .serve

  private lazy val priceService: PriceService[IO] =
    PriceService(Dependencies[IO], Logger[IO])
}
```
