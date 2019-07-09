# Http4s Poc Api
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/117ccce325c04aada3d56a657aab170c)](https://app.codacy.com/app/barambani/http4s-poc-api?utm_source=github.com&utm_medium=referral&utm_content=barambani/http4s-poc-api&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.org/barambani/http4s-poc-api.svg?branch=master)](https://travis-ci.org/barambani/http4s-poc-api)
[![codecov](https://codecov.io/gh/barambani/http4s-poc-api/branch/master/graph/badge.svg)](https://codecov.io/gh/barambani/http4s-poc-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/barambani/http4s-poc-api/blob/master/LICENSE)

This repo contains a complete example of a http api implemented with [http4s](http://http4s.org/) and running on [Zio](https://github.com/zio/zio) in a tagless final encoding.

## How to run it
To run the service locally on docker the following steps are needed:
#### Tag
To get a correct image tag the repo's `HEAD` needs to have a tag. If that's not the case a local tag could be added with something like
```
git tag v0.0.1-Test
```
#### Build docker image
to create the image the most straightforward option is to use the sbt plugin that comes with the project
```
sbt docker:publishLocal
```
if that's successful, looking up for the images
```
docker images
```
should give something like
```
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
http4s-poc-api      0.0.1-Test          4a3ba2767d13        14 minutes ago      521MB
[...]
```
#### Run
if the image has been created successfully the last step is to run it
```
docker run -p 17171:17171 4a3ba2767
```
clearly the ids will be different but the server should start with a message like
```
[2019-07-09 13:03:38,875][INFO ][zio-default-async-1-2104457164][o.h.b.c.nio1.NIO1SocketServerGroup] Service bound to address /0.0.0.0:17171
[2019-07-09 13:03:38,896][DEBUG][blaze-selector-0][o.h.blaze.channel.nio1.SelectorLoop] Channel initialized.
[2019-07-09 13:03:38,911][INFO ][zio-default-async-1-2104457164][o.h.server.blaze.BlazeServerBuilder]
  _   _   _        _ _
 | |_| |_| |_ _ __| | | ___
 | ' \  _|  _| '_ \_  _(_-<
 |_||_\__|\__| .__/ |_|/__/
             |_|
[2019-07-09 13:03:39,054][INFO ][zio-default-async-1-2104457164][o.h.server.blaze.BlazeServerBuilder] http4s v0.20.4 on blaze v0.14.5 started at http://0.0.0.0:17171/
```
to verify the service, a curl call can be executed as below (notice the below uses `jq` but should work regardless)
```
curl http://127.0.0.1:17171/pricing-api/health-check | jq
```
with a response (trhough `jq`) on the line of
```
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   146  100   146    0     0    151      0 --:--:-- --:--:-- --:--:--   151
{
  "name": "http4s-poc-api",
  "version": "0.0.1-Test",
  "scalaVersion": "2.12.8",
  "scalaOrganization": "org.scala-lang",
  "buildTime": "2019-07-09T12:44:56.844Z"
}
```

## Service Structure

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
where the capabilities of `F[_]` are provided through implicit evidences
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
Sometimes is also usefull to fire some external dependencies in parallel when there is a collection of known coordinates as source. In this repo for instance, this is the case when the service needs to check the cache for a list of `ProductId` and collect their details in case they exist. As before, this computation can be described again at a high level of abstraction using `ParEffectful` and its `parTraverse` function ([see the implementation here](https://github.com/barambani/http4s-extend/blob/master/src/main/scala/http4s/extend/ParEffectful.scala#L60)).
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
This approach decouples very well the details of the execution and the decoding/encoding from the domain logic's formalization. With this style is possible to describe at a very high level of abstraction the expected behavior of the system and the domain context at hand, tuning the power of the structures needed by any implementation in a particurarly fine way. The sole place where the actual runtime becomes relevant is in the `Main` server file where all the instances are materialized (notice all the occurrencies of IO below that don't appear in the internals of the service).
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
