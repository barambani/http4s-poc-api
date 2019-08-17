# Http4s Poc Api
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/117ccce325c04aada3d56a657aab170c)](https://app.codacy.com/app/barambani/http4s-poc-api?utm_source=github.com&utm_medium=referral&utm_content=barambani/http4s-poc-api&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.org/barambani/http4s-poc-api.svg?branch=master)](https://travis-ci.org/barambani/http4s-poc-api)
[![codecov](https://codecov.io/gh/barambani/http4s-poc-api/branch/master/graph/badge.svg)](https://codecov.io/gh/barambani/http4s-poc-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/barambani/http4s-poc-api/blob/master/LICENSE)

This repo contains an example of http api implemented with [http4s](http://http4s.org/) and running on [Zio](https://github.com/zio/zio) in a tagless final encoding.

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
The logic is encoded as a tagless final DSL that abstracts over the concrete functional effect.

```scala
/**
 * the DSL is encoded as a set of traits
 */
sealed trait PreferenceFetcher[F[_]] {
  def userPreferences: UserId => F[UserPreferences]
}
```
where the capabilities of `F[_]` are provided through implicit evidences and type-classes
```scala
@inline def apply[F[_]](dep: UserIntegration[F], log: LogWriter[F])(
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

    private def validate(p: UserPreferences, id: UserId): F[UserPreferences] = [...]
  }
```

#### Parallel Execution
When there's the need of running computations (in the general sense, so possibly effectful) in parallel, the same approach is used. This capability is enabled through the evidence that `F[_]: Parallel[?[_], ParTask]` so that `parTraverse` is made available to the implementation.
```scala
@inline def apply[F[_]: Monad: Parallel[?[_], ParTask]](
  productStore: ProductIntegration[F],
  logger: LogWriter[F]
): PriceCalculator[F] =
  new PriceCalculator[F] {

    def finalPrices(user: User, prods: Seq[Product], pref: UserPreferences): F[List[Price]] =
      prods.toList.parTraverse(userPrice(pref, user))

    private def userPrice: (UserPreferences, User) => Product => F[Price] =
      (prefs, user) =>
        product =>
          for {
            catalogPrice <- productStore.productPrice(product)(prefs) <*
                             logger.debug(s"Catalog price of ${product.id} collected")
            userPrice = veryVeryComplexPureCalculation(catalogPrice)(user.userPurchaseHistory)
            _         <- logger.debug(s"Price calculation for product ${product.id} completed")
          } yield userPrice

    private def veryVeryComplexPureCalculation: Price => Seq[UserPurchase] => Price =
      price => _ => price
  }
```
The same pattern applies when there's the need to look-up some external dependencies in parallel given a collection of known coordinates as source. This is the case, for instance, when the execution requires to check the cache for a list of `ProductId` and to collect eventual details in case they exist. As before, this behavior can be enabled constraining `F[_]` to the evidence of `Parallel[?[_], ParTask]` and using its `parTraverse` function (see [here](https://github.com/barambani/http4s-poc-api/blob/master/src/main/scala/service/ProductRepo.scala#L23) the full implementation).
```scala
@inline def apply[F[_]: Monad: Parallel[?[_], ParTask]](
  cache: CacheIntegration[F],
  productInt: ProductIntegration[F],
  logger: LogWriter[F]
): ProductRepo[F] =
  new ProductRepo[F] {

    /**
      * Tries to retrieve the products by ProductId from the cache, if results
      * in a miss it tries on the http product store.
      * It returns only the products existing so the result might contain less
      * elements than the input list. If a product is not in the cache but is
      * found in the http store it will be added to the cache.
      */
    def storedProducts: Seq[ProductId] => F[List[Product]] =
      _.toList parTraverse fromCacheOrStore map (_.flatten)

    private[this] def fromCacheOrStore: ProductId => F[Option[Product]] =
      id => cache.cachedProduct(id) >>= cacheMissFetch(id)

    private[this] def cacheMissFetch: ProductId => Option[Product] => F[Option[Product]] = [...]
  }
```

#### Http Endpoints
The http routes are implemented following the same style. The capabilities of `F[_]` are described through type-classes. An example of that is the way evidences of `Decoder` for the request payloads and of the `Encoder` for the response body are provided. This describes well everything that `F[_]` will have to guarantee to make the endpoint implementation doable.
```scala
sealed abstract class PriceRoutes[F[_]: Sync](
  implicit
  requestDecoder: EntityDecoder[F, PricesRequestPayload],
  responseEncoder: EntityEncoder[F, List[Price]],
) extends Http4sDsl[F] {

  def make(priceService: PriceService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ Method.POST -> Root =>
        postResponse(req, priceService) handlingFailures priceServiceErrors handleErrorWith unhandledThrowable
    }

  private[this] def postResponse(request: Request[F], priceService: PriceService[F]): F[Response[F]] =
    for {
      payload <- request.as[PricesRequestPayload]
      prices  <- priceService.prices(payload.userId, payload.productIds)
      resp    <- Ok(prices)
    } yield resp

  private[this] def priceServiceErrors: PriceServiceError => F[Response[F]] = {
    case UserErr(r)                => FailedDependency(r)
    case PreferenceErr(r)          => FailedDependency(r)
    case ProductErr(r)             => FailedDependency(r)
    case ProductPriceErr(r)        => FailedDependency(r)
    case CacheLookupError(r)       => FailedDependency(r)
    case CacheStoreError(r)        => FailedDependency(r)
    case InvalidShippingCountry(r) => BadRequest(r)
  }

  private[this] def unhandledThrowable: Throwable => F[Response[F]] = { th =>
    import external.library.instances.throwable._
    InternalServerError(th.show)
  }
}
```

#### Main Server
The described approach decouples very well the details of the actual execution (logging, settings collection) and of the objects decoding/encoding from the domain logic's formalization. With this style (tagless final) is possible to describe at a very high level of abstraction the expected behavior of the parametric functional effect, refining the power of the structure to the minimum required by the implementations, and this description can always be verified by the compiler in an automatic way. The sole place where the actual runtime effect becomes relevant is in the `Main` server file where all the instances are materialized (notice all the occurrences of all the RIO and ZIO and notice the specialisation of the `zio.interop.catz.CatsApp` that don't appear in any other part of the implementation and don't pollute the internals of the actual business logic itself).
```scala
object Main extends zio.interop.catz.CatsApp with RuntimeThreadPools with Codecs {

  private[this] val priceService: RIO[String, PriceService[Task]] =
    log4sFromName map { log =>
      PriceService[Task](
        TeamThreeCacheApi.productCache,
        TeamOneHttpApi(),
        TeamTwoHttpApi(),
        log
      )
    }

  private[this] val httpApp: RIO[String, HttpApp[Task]] =
    priceService map { ps =>
      Router(
        "/pricing-api/prices"       -> PriceRoutes[Task].make(ps),
        "/pricing-api/health-check" -> HealthCheckRoutes[Task].make(ps.logger)
      ).orNotFound
    }

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (httpApp.provide("App log") >>= { app =>
      BlazeServerBuilder[Task]
        .bindHttp(17171, "0.0.0.0")
        .withConnectorPoolSize(64)
        .enableHttp2(true)
        .withHttpApp(app)
        .serve
        .compile
        .drain
    }).fold(_ => 0, _ => 1)
}
```
