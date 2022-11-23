# This repository is ⚰️ ARCHIVED ⚰️

The last day of operation was on November 23, 2022

</br>

# Http4s Poc Api

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
#### Build a docker image
to create the image the most straightforward option is to use the sbt plugin that comes with the project
```
sbt docker:publishLocal
```
if that's successful, looking up for the images with
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
#### Verify
to verify the service, you can send a curl request like the below (notice that the example uses `jq` but it's not required)
```
curl http://127.0.0.1:17171/pricing-api/health-check | jq
```
with a response (through `jq`) on the line of
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

    private def veryVeryComplexPureCalculation: Price => Seq[UserPurchase] => Price = [...]
  }
```
The same pattern applies when there's the need to look-up some external dependencies in parallel given a collection of known coordinates as source. This is the case, for instance, when the execution requires to check the cache for a list of `ProductId` and to collect eventual details if they exist. As before, this behavior can be enabled constraining `F[_]` to the evidence of `Parallel[?[_], ParTask]` and using its `parTraverse` function (see [here](https://github.com/barambani/http4s-poc-api/blob/master/src/main/scala/service/ProductRepo.scala#L23) the full implementation).
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

#### Http Routes
The http routes are implemented following the same style. The capabilities of `F[_]` are described through type-classes. An example of that is the way evidences of `EntityDecoder` for the request payloads, of the `EntityEncoder` for the response body and of `Sync` execution are provided. This describes well everything that `F[_]` will have to guarantee to make the route's implementation possible.
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
The described approach decouples very well the details of the actual execution (logging, settings collection) and those of the decoding/encoding from the domain logic's formalisation. With this style (tagless final) it's possible to describe at a very high level of abstraction the expected behavior of the parametric functional effect, refining the power of the effect's structure to the minimum required by the implementations. Everything always verified by the compiler in an automatic fashion. The only place where the actual runtime system becomes relevant is the `Main` server file where all the instances are materialized (notice all the occurrences of RIO and ZIO and the specialisation of the `zio.interop.catz.CatsApp` that don't appear in any other part of the implementation and that don't pollute the internals of the actual business logic itself).

As a last note, below you can also see how simple and denotational the definition of the runtime can be if the effect system in use is as powerful and descriptive as `Zio`. Note, in fact, how you can build separately the different runtime components and how nicely and easily you can assemble them thanks to `RIO`, so that the only thing left to do in `run` is to adapt the output and provide the requirements.
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

  private[this] val httpApp: RIO[PriceService[Task], HttpApp[Task]] =
    ZIO.access { ps =>
      Router(
        "/pricing-api/prices"       -> PriceRoutes[Task].make(ps),
        "/pricing-api/health-check" -> HealthCheckRoutes[Task].make(ps.logger)
      ).orNotFound
    }

  private[this] val runningServer: RIO[HttpApp[Task], Unit] =
    ZIO.accessM { app =>
      BlazeServerBuilder[Task]
        .bindHttp(17171, "0.0.0.0")
        .withConnectorPoolSize(64)
        .enableHttp2(true)
        .withHttpApp(app)
        .serve
        .compile
        .drain
    }

  private[this] val serviceRuntime: RIO[String, Unit] =
    priceService >>> httpApp >>> runningServer

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    serviceRuntime.fold(_ => 0, _ => 1) provide "App log"
}
```

### Notes
The repo's [library](https://github.com/barambani/http4s-poc-api/tree/master/src/main/scala/external/library) folder contains some helpers that are not strictly needed to create the service and they help reducing the noise in the actual service code. The structure above could be created also without using these modules.

#### Newtypes
The [newtype](https://github.com/barambani/http4s-poc-api/blob/master/src/main/scala/external/library/newtype.scala) trait is a building block to help create zero allocation new types like
```scala
object MkAndBoolean extends newtype[Boolean]

val AndBoolean = MkAndBoolean
type AndBoolean = AndBoolean.T
```
This tecnique is a great help when trying to avoid orphan type class instances for, creating a `newtype`, allows to have eventual instances in the implicit scope even when the type of interest and the type class itself are owned by someone else and their companion objects cannot be changed. Having this possibility without paying an allocation cost per use is very desirable and cannot be achieved with the language's Value Classes. Considering this example in fact
```scala
class ValueClass(val v: Boolean) extends AnyVal

class testNewType {

  val ntA1 = AndBoolean(true)
  val ntA2 = AndBoolean(false)

  val ntTuple = (ntA1, ntA2)

  val ntLs = List(ntA1, ntA2)

  val ntId = identity(ntA1)
}

class testValueClass {

  val vcA1 = new ValueClass(true)
  val vcA2 = new ValueClass(false)

  val vcTuple = (vcA1, vcA2)

  val vcLs = List(vcA1, vcA2)

  val vcId = identity(vcA1)
}
```
and giving a look at its disassembled code we can see how `NewType`'s approach differs from the Value Classes in terms of allocations (see how the occurrences of `new` differ).

**Tuple**
```scala
public http4s.extend.testValueClass();
  descriptor: ()V
    Code:
      37: new           #54                 // class scala/Tuple2
      40: dup

      41: new           #81                 // class http4s/extend/ValueClass
      44: dup
      45: aload_0
      46: invokevirtual #83                 // Method vcA1:()Z
      49: invokespecial #86                 // Method http4s/extend/ValueClass."<init>":(Z)V

      52: new           #81                 // class http4s/extend/ValueClass
      55: dup
      56: aload_0
      57: invokevirtual #88                 // Method vcA2:()Z
      60: invokespecial #86                 // Method http4s/extend/ValueClass."<init>":(Z)V

      63: invokespecial #91                 // Method scala/Tuple2."<init>":(Ljava/lang/Object;Ljava/lang/Object;)V
      66: putfield      #50                 // Field vcTuple:Lscala/Tuple2;


public http4s.extend.testNewType();
  descriptor: ()V
    Code:
      55: new           #59                 // class scala/Tuple2
      58: dup

      59: aload_0
      60: invokevirtual #116                // Method ntA1:()Ljava/lang/Object;

      63: aload_0
      64: invokevirtual #118                // Method ntA2:()Ljava/lang/Object;

      67: invokespecial #121                // Method scala/Tuple2."<init>":(Ljava/lang/Object;Ljava/lang/Object;)V
      70: putfield      #55                 // Field ntTuple:Lscala/Tuple2;
```
**List**
```scala
public http4s.extend.testValueClass();
    descriptor: ()V
    Code:
      81: getstatic     #97                 // Field scala/collection/immutable/List$.MODULE$:Lscala/collection/immutable/List$;
      84: getstatic     #102                // Field scala/Predef$.MODULE$:Lscala/Predef$;
      87: iconst_2
      88: anewarray     #81                 // class http4s/extend/ValueClass
      91: dup
      92: iconst_0
      
      93: new           #81                 // class http4s/extend/ValueClass
      96: dup
      97: aload_0
      98: invokevirtual #83                 // Method vcA1:()Z
     101: invokespecial #86                 // Method http4s/extend/ValueClass."<init>":(Z)V
     104: aastore
     
     105: dup
     106: iconst_1
     
     107: new           #81                 // class http4s/extend/ValueClass
     110: dup
     111: aload_0
     112: invokevirtual #88                 // Method vcA2:()Z
     115: invokespecial #86                 // Method http4s/extend/ValueClass."<init>":(Z)V
     118: aastore
     
     119: invokevirtual #106                // Method scala/Predef$.genericWrapArray:(Ljava/lang/Object;)Lscala/collection/mutable/WrappedArray;
     122: invokevirtual #110                // Method scala/collection/immutable/List$.apply:(Lscala/collection/Seq;)Lscala/collection/immutable/List;
     125: putfield      #57                 // Field vcLs:Lscala/collection/immutable/List;


public http4s.extend.testNewType();
    descriptor: ()V
    Code:
      85: getstatic     #126                // Field scala/collection/immutable/List$.MODULE$:Lscala/collection/immutable/List$;
      88: getstatic     #131                // Field scala/Predef$.MODULE$:Lscala/Predef$;
      91: iconst_2
      92: anewarray     #4                  // class java/lang/Object
      
      95: dup
      96: iconst_0
      97: aload_0
      98: invokevirtual #116                // Method ntA1:()Ljava/lang/Object;
     101: aastore
     
     102: dup
     103: iconst_1
     104: aload_0
     105: invokevirtual #118                // Method ntA2:()Ljava/lang/Object;
     108: aastore
     
     109: invokevirtual #135                // Method scala/Predef$.genericWrapArray:(Ljava/lang/Object;)Lscala/collection/mutable/WrappedArray;
     112: invokevirtual #138                // Method scala/collection/immutable/List$.apply:(Lscala/collection/Seq;)Lscala/collection/immutable/List;
     115: putfield      #62                 // Field ntLs:Lscala/collection/immutable/List;
```
**Identity**
```scala
public http4s.extend.testValueClass();
    descriptor: ()V
    Code:
      141: getstatic     #102                // Field scala/Predef$.MODULE$:Lscala/Predef$;
      
      144: new           #81                 // class http4s/extend/ValueClass
      147: dup
      148: aload_0
      149: invokevirtual #83                 // Method vcA1:()Z
      152: invokespecial #86                 // Method http4s/extend/ValueClass."<init>":(Z)V
      155: invokevirtual #114                // Method scala/Predef$.identity:(Ljava/lang/Object;)Ljava/lang/Object;
      158: checkcast     #81                 // class http4s/extend/ValueClass
      
      161: invokevirtual #117                // Method http4s/extend/ValueClass.v:()Z
      164: putfield      #63                 // Field vcId:Z


public http4s.extend.testNewType();
    descriptor: ()V
    Code:
      131: getstatic     #131                // Field scala/Predef$.MODULE$:Lscala/Predef$;
      
      134: aload_0
      135: invokevirtual #116                // Method ntA1:()Ljava/lang/Object;
      
      138: invokevirtual #141                // Method scala/Predef$.identity:(Ljava/lang/Object;)Ljava/lang/Object;
      141: putfield      #68                 // Field ntId:Ljava/lang/Object;
```
Even if this approach is understandably the best compromise available in Scala at the moment, it comes at a cost. When the `newtype`s are used on value types in fact, they are not erased to the original base type as it's the case for Value Classes, but they are erased to `Object` as can be seen from the disassembled code above and from the snippet below
```java
public boolean vcA1();
  descriptor: ()Z

public boolean vcA2();
  descriptor: ()Z
```
```java
public java.lang.Object ntA1();
  descriptor: ()Ljava/lang/Object;

public java.lang.Object ntA2();
  descriptor: ()Ljava/lang/Object;
```
