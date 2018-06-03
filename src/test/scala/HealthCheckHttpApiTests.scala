import cats.effect.IO
import cats.instances.string._
import cats.syntax.apply._
import http4s.extend.syntax.httpService._
import http4s.extend.syntax.responseVerification._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import model.DomainModel.ServiceSignature
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{HttpService, Status}
import org.scalatest.{FlatSpec, Matchers}
import server.HealthCheckHttpApi

final class HealthCheckHttpApiTests extends FlatSpec with Matchers with Fixtures {

  import EitherHtt4sClientDsl._
  import EitherHttp4sDsl._

  implicit def testEncoder[A : Encoder] = jsonEncoderOf[IO, A]
  implicit def testDecoder[A : Decoder] = jsonOf[IO, A]

  it should "respond with Ok status 200 and the correct service signature" in {

    val httpApi: HttpService[IO] =
      HealthCheckHttpApi[IO].service()

    val request = GET(uri("/"))

    val verified = httpApi.runForF(request).verify[ServiceSignature](
      Status.Ok,
      sign =>
        (sign.name              isSameAs    "http4s-poc-api",
        sign.version            isNotSameAs "",
        sign.scalaVersion       isSameAs    "2.12.6",
        sign.scalaOrganization  isSameAs    "org.scala-lang").mapN((_, _, _, _) => sign)
    )

    assertOn(verified)
  }
}