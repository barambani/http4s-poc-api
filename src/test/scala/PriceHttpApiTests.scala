import java.time.Instant

import cats.instances.either._
import cats.syntax.validated._
import errors.ApiError
import http4s.extend.instances.invariant._
import http4s.extend.syntax.httpService._
import http4s.extend.syntax.responseVerification._
import http4s.extend.util.EntityDecoderModule.eitherEntityDecoder
import http4s.extend.util.EntityEncoderModule.eitherEntityEncoder
import instances.ErrorMapInstances._
import interpreters.TestDependencies._
import interpreters.TestLogger._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import model.DomainModel._
import model.DomainModelSyntax._
import org.http4s.{HttpService, Status}
import org.scalatest.{FlatSpec, Matchers}
import server.PriceHttpApi
import service.PriceService
import codecs.ServiceCodec._

final class PriceHttpApiTests extends FlatSpec with Matchers with Fixtures {

  import EitherHtt4sClientDsl._
  import EitherHttp4sDsl._

  implicit def errorEncoder[A : Encoder] = eitherEntityEncoder[ApiError, A]
  implicit def errorDecoder[A : Decoder] = eitherEntityDecoder[ApiError, A]

  val aUser       = User(111.asUserId, Nil)
  val aProduct    = Product(123.asProductId, "some spec".asProductSpec, Nil)
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

    val pricing: PriceService[Either[ApiError, ?]] =
      PriceService[Either[ApiError, ?]](
        testSucceedingDependencies(aUser, preferences, aProduct, price),
        testLogger
      )

    val httpApi: HttpService[Either[ApiError, ?]] =
      PriceHttpApi[Either[ApiError, ?]].service(pricing)

    val reqPayload = PricesRequestPayload(
      17.asUserId,
      Seq(123.asProductId, 456.asProductId)
    )

    val request = POST(uri("/"), reqPayload.asJson)

    val verified = httpApi.runForF(request).verify[Seq[Price]](
      Status.Ok,
      ps =>
        if(ps.lengthCompare(reqPayload.productIds.size) == 0) ps.validNel
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

    val pricing: PriceService[Either[ApiError, ?]] =
      PriceService[Either[ApiError, ?]](
        testSucceedingDependencies(aUser, wrongPreferences, aProduct, price),
        testLogger
      )

    val httpApi: HttpService[Either[ApiError, ?]] =
      PriceHttpApi[Either[ApiError, ?]].service(pricing)

    val reqPayload = PricesRequestPayload(
      17.asUserId,
      Seq(123.asProductId, 456.asProductId)
    )

    val request = POST(uri("/"), reqPayload.asJson)

    val verified = httpApi.runForF(request).verifyResponseText(
      Status.InternalServerError,
      "Service Error: InvalidShippingCountry: Cannot ship outside Italy"
    )

    assertOn(verified)
  }

  it should "respond with Bad gateway 502 for dependent service failure" in {

    val pricing: PriceService[Either[ApiError, ?]] =
      PriceService[Either[ApiError, ?]](testFailingDependencies, testLogger)

    val httpApi: HttpService[Either[ApiError, ?]] =
      PriceHttpApi[Either[ApiError, ?]].service(pricing)

    val reqPayload = PricesRequestPayload(
      17.asUserId,
      Seq(123.asProductId, 456.asProductId)
    )

    val request = POST(uri("/"), reqPayload.asJson)

    val verified = httpApi.runForF(request).verifyResponseText(
      Status.BadGateway,
      "Service Error: DependencyFailure. The dependency def user: UserId => Either[ApiError, User] failed with message network failure"
    )

    assertOn(verified)
  }
}