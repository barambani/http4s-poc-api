import java.time.Instant

import cats.syntax.validated._
import interpreters.{ TestTeamOneHttpApi, TestTeamThreeCacheApi, TestTeamTwoHttpApi }
import io.circe.generic.auto._
import io.circe.{ Decoder, Encoder }
import model.DomainModel._
import model.DomainModelSyntax._
import org.http4s.circe.{ jsonEncoderOf, jsonOf }
import org.http4s.{ Method, Request, Status }
import org.scalatest.flatspec.AnyFlatSpec
import server.PriceRoutes
import service.PriceService
import syntax.http4sService._
import syntax.responseVerification._
import zio.Task
import zio.interop.catz._
import zio.interop.catz.implicits._
import model.DomainModelCodecs._
import org.scalatest.matchers.should.Matchers

final class PriceHttpApiTests extends AnyFlatSpec with Matchers with Fixtures {

  implicit def testEncoder[A: Encoder] = jsonEncoderOf[Task, A]
  implicit def testDecoder[A: Decoder] = jsonOf[Task, A]

  val aUser = User(111.asUserId, Nil)

  val productsInStore = Map(
    456.asProductId -> Product(456.asProductId, "some other spec".asProductSpec, Nil)
  )

  val productsInCache = Map(
    123.asProductId -> Product(123.asProductId, "some spec".asProductSpec, Nil)
  )

  val price = Price(
    amount = BigDecimal(2.34).asMoneyAmount,
    currency = "EUR".asCurrency,
    discount = None,
    priceTimeStamp = Instant.now()
  )

  val preferences = UserPreferences(
    destination = ShipmentDestination("address".asUserAddress, "Italy".asCountry),
    currency = "EUR".asCurrency
  )

  it should "respond with Ok 200 and the correct number of prices" in {

    val pricing = PriceService[Task](
      TestTeamThreeCacheApi.make(productsInCache)(testLog),
      TestTeamOneHttpApi.make(preferences, price),
      TestTeamTwoHttpApi.make(aUser, productsInStore)(testLog),
      testLog
    )

    val httpApi = PriceRoutes[Task].make(pricing)

    val reqPayload = PricesRequestPayload(
      17.asUserId,
      Seq(123.asProductId, 456.asProductId, 171.asProductId)
    )

    val request = Request[Task](method = Method.POST).withEntity(reqPayload)

    val verified = httpApi
      .runFor(request)
      .verify[Seq[Price]](
        Status.Ok,
        ps =>
          if (ps.size == 2) ps.validNel
          else s"Wrong number of prices. Expected ${reqPayload.productIds.size} but was ${ps.size}".invalidNel
      )

    assertOn(verified)
  }

  it should "respond with Status 400 for invalid shipping country" in {

    val wrongPreferences = preferences.copy(
      destination = preferences.destination.copy(
        country = "NotItaly".asCountry
      )
    )

    val pricing = PriceService[Task](
      TestTeamThreeCacheApi.make(productsInCache)(testLog),
      TestTeamOneHttpApi.make(wrongPreferences, price),
      TestTeamTwoHttpApi.make(aUser, productsInStore)(testLog),
      testLog
    )

    val httpApi = PriceRoutes[Task].make(pricing)

    val reqPayload = PricesRequestPayload(
      18.asUserId,
      Seq(123.asProductId, 456.asProductId, 171.asProductId)
    )

    val request = Request[Task](method = Method.POST).withEntity(reqPayload)

    val verified = httpApi
      .runFor(request)
      .verifyResponseText(
        Status.BadRequest,
        "InvalidShippingCountry: Cannot ship outside Italy"
      )

    assertOn(verified)
  }

  it should "respond with Status 502 for multiple dependency failures" in {

    val pricing = PriceService[Task](
      TestTeamThreeCacheApi.makeFail,
      TestTeamOneHttpApi.makeFail,
      TestTeamTwoHttpApi.makeFail,
      testLog
    )

    val httpApi = PriceRoutes[Task].make(pricing)

    val reqPayload = PricesRequestPayload(
      19.asUserId,
      Seq(123.asProductId, 456.asProductId, 171.asProductId)
    )

    val request = Request[Task](method = Method.POST).withEntity(reqPayload)

    val verified = httpApi
      .runFor(request)
      .verifyResponseText(
        Status.FailedDependency,
        "DependencyFailure. The dependency `UserId => Future[UserPreferences]` failed with message timeout"
      )

    assertOn(verified)
  }
}
