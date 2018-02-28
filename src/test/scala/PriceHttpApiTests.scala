import java.time.Instant
import java.util.concurrent.ForkJoinPool

import cats.effect.IO
import cats.syntax.validated._
import errors.ThrowableInstances._
import http4s.extend.syntax.httpService._
import http4s.extend.syntax.responseVerification._
import interpreters.TestDependencies._
import interpreters.TestLogger._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import model.DomainModel._
import model.DomainModelSyntax._
import org.http4s.Status
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.scalatest.{FlatSpec, Matchers}
import server.PriceHttpApi
import service.PriceService
import model.DomainModelCodecs._

import scala.concurrent.ExecutionContext

final class PriceHttpApiTests extends FlatSpec with Matchers with Fixtures {

  import EitherHtt4sClientDsl._
  import EitherHttp4sDsl._

  implicit val testExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new ForkJoinPool())

  implicit def testEncoder[A : Encoder] = jsonEncoderOf[IO, A]
  implicit def testDecoder[A : Decoder] = jsonOf[IO, A]

  val aUser = User(111.asUserId, Nil)

  val productsInStore = Map(
    456.asProductId -> Product(456.asProductId, "some other spec".asProductSpec, Nil)
  )

  val productsInCache = Map(
    123.asProductId -> Product(123.asProductId, "some spec".asProductSpec, Nil)
  )

  val price       = Price(
    amount          = BigDecimal(2.34).asMoneyAmount,
    currency        = "EUR".asCurrency,
    discount        = None,
    priceTimeStamp  = Instant.now()
  )

  val preferences = UserPreferences(
    destination = ShipmentDestination("address".asUserAddress, "Italy".asCountry),
    currency    = "EUR".asCurrency
  )

  it should "respond with Ok 200 and the correct number of prices" in {

    val pricing = PriceService[IO](
      testSucceedingDependencies(aUser, preferences, productsInStore, productsInCache, price),
      testLogger
    )

    val httpApi = PriceHttpApi[IO].service(pricing)

    val reqPayload = PricesRequestPayload(
      17.asUserId,
      Seq(123.asProductId, 456.asProductId, 171.asProductId)
    )

    val request = POST(uri("/"), reqPayload.asJson)

    val verified = httpApi.runForF(request).verify[Seq[Price]](
      Status.Ok,
      ps =>
        if(ps.lengthCompare(2) == 0) ps.validNel
        else s"Wrong number of prices. Expected ${reqPayload.productIds.size} but was ${ps.size}".invalidNel
    )

    assertOn(verified)
  }

  it should "respond with Status 500 for invalid shipping country" in {

    val wrongPreferences = preferences.copy(
      destination = preferences.destination.copy(
        country = "NotItaly".asCountry
      )
    )

    val pricing = PriceService[IO](
      testSucceedingDependencies(aUser, wrongPreferences, productsInStore, productsInCache, price),
      testLogger
    )

    val httpApi = PriceHttpApi[IO].service(pricing)

    val reqPayload = PricesRequestPayload(
      18.asUserId,
      Seq(123.asProductId, 456.asProductId, 171.asProductId)
    )

    val request = POST(uri("/"), reqPayload.asJson)

    val verified = httpApi.runForF(request).verifyResponseText(
      Status.InternalServerError,
      "InvalidShippingCountry: Cannot ship outside Italy"
    )

    assertOn(verified)
  }

  it should "respond with Status 500 for multiple dependency failures" in {

    val pricing = PriceService[IO](testFailingDependencies, testLogger)

    val httpApi = PriceHttpApi[IO].service(pricing)

    val reqPayload = PricesRequestPayload(
      19.asUserId,
      Seq(123.asProductId, 456.asProductId, 171.asProductId)
    )

    val request = POST(uri("/"), reqPayload.asJson)

    val verified = httpApi.runForF(request).verifyResponseText(
      Status.InternalServerError,
      """DependencyFailure. The dependency def user: UserId => IO[User] failed with message network failure
        |DependencyFailure. The dependency def cachedProduct: ProductId => IO[Option[Product]] failed with message not responding
        |DependencyFailure. The dependency def usersPreferences: UserId => IO[UserPreferences] failed with message timeout""".stripMargin
    )

    assertOn(verified)
  }
}