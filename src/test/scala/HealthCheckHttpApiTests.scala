import cats.instances.either._
import cats.instances.string._
import cats.syntax.apply._
import errors.ApiError
import http4s.extend.instances.invariant._
import http4s.extend.syntax.httpService._
import http4s.extend.syntax.responseVerification._
import http4s.extend.util.EntityDecoderModule.eitherEntityDecoder
import http4s.extend.util.EntityEncoderModule.eitherEntityEncoder
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import model.DomainModel.ServiceSignature
import org.http4s.{HttpService, Status}
import org.scalatest.{FlatSpec, Matchers}
import server.HealthCheckHttpApi

final class HealthCheckHttpApiTests extends FlatSpec with Matchers with Fixtures {

  import EitherHtt4sClientDsl._
  import EitherHttp4sDsl._

  implicit def errorEncoder[A : Encoder] = eitherEntityEncoder[ApiError, A]
  implicit def errorDecoder[A : Decoder] = eitherEntityDecoder[ApiError, A]

  it should "respond with Ok status 200 and the correct service signature" in {

    val httpApi: HttpService[Either[ApiError, ?]] =
      HealthCheckHttpApi[Either[ApiError, ?]].service()

    val request = GET(uri("/"))

    val verified = httpApi.runForF(request).verify[ServiceSignature](
      Status.Ok,
      sign =>
        (sign.name              isSameAs "http4s-poc-api",
        sign.version            isNotSameAs "",
        sign.scalaVersion       isSameAs "2.12.4-bin-typelevel-4",
        sign.scalaOrganization  isSameAs "org.typelevel").mapN((_, _, _, _) => sign)
    )

    assertOn(verified)
  }
}