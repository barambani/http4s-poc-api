import cats.instances.either._
import cats.syntax.validated._
import errors.ApiError
import http4s.extend.syntax.httpService._
import http4s.extend.syntax.responseVerification._
import http4s.extend.util.EntityDecoderModule.eitherEntityDecoder
import http4s.extend.util.EntityEncoderModule.eitherEntityEncoder
import instances.ErrorConversionInstances._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import model.DomainModel.{Price, PricesRequestPayload}
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

  it should "respond with Ok 200 and the correct number of prices" in {

    val pricing: PriceService[Either[ApiError, ?]] =
      PriceService[Either[ApiError, ?]](testSucceedingDependencies, testLogger)

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

//  it should "respond with Bad request 400 for Invalid params when the anonymous context in" in {
//
//    val dependencies: CommonsLibrary[Either[ApiError, ?]] =
//      succeedingDependencies(testProducts, new LocalizationPreferenceImpl)
//
//    val pricing: PricingService[Either[ApiError, ?]] =
//      PricingService(dependencies, testCache)
//
//    val httpApi: HttpService[Either[ApiError, ?]] =
//      PricingHttpApi[Either[ApiError, ?]].service(pricing)
//
//    val reqPayload = PricingRequest(
//      Seq[Long](123),
//      AnonymousContext("aaaa", "ffff", "gggg")
//    )
//
//    val request = POST(uri("/"), reqPayload.asJson)
//
//    httpApi.runForF(request).verifyResponseText(
//      Status.BadRequest,
//      msg => msg should be(
//        "Service Error: InvalidParams, Unrecognised params passed to the api currency = aaaa, country = ffff, locale = gggg"
//      )
//    )
//  }
//
//  it should "respond with Bad gateway 502 for dependent service failure" in {
//
//    val dependencies: CommonsLibrary[Either[ApiError, ?]] =
//      failingDependencies
//
//    val pricing: PricingService[Either[ApiError, ?]] =
//      PricingService(dependencies, testCache)
//
//    val httpApi: HttpService[Either[ApiError, ?]] =
//      PricingHttpApi[Either[ApiError, ?]].service(pricing)
//
//    val reqPayload = PricingRequest(Seq[Long](123), UserRelatedContext(UUID.randomUUID()))
//
//    val request = POST(uri("/"), reqPayload.asJson)
//
//    httpApi.runForF(request).verifyResponseText(
//      Status.BadGateway,
//      msg => msg should be(
//        "Service Error: DependentServiceFailure, Test failure for userPreference"
//      )
//    )
//  }
//
//  it should "respond with Internal service error 500 for undefined context" in {
//
//    val dependencies: CommonsLibrary[Either[ApiError, ?]] =
//      succeedingDependencies(testProducts, new LocalizationPreferenceImpl)
//
//    val pricing: PricingService[Either[ApiError, ?]] =
//      PricingService(dependencies, testCache)
//
//    val httpApi: HttpService[Either[ApiError, ?]] =
//      PricingHttpApi[Either[ApiError, ?]].service(pricing)
//
//    val reqPayload = PricingRequest(Seq[Long](123), PricingContextUndefinedType("undefined context"))
//
//    val request = POST(uri("/"), reqPayload.asJson)
//
//    httpApi.runForF(request).verifyResponseText(
//      Status.InternalServerError,
//      msg => msg should be(
//        "Service Error: ServiceFailure, Unable to process context"
//      )
//    )
//  }

}