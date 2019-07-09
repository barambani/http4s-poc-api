import cats.instances.string._
import cats.syntax.apply._
import io.circe.generic.auto._
import io.circe.{ Decoder, Encoder }
import model.DomainModel.ServiceSignature
import org.http4s.circe.{ jsonEncoderOf, jsonOf }
import org.http4s.{ HttpRoutes, Request, Status }
import org.scalatest.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import server.HealthCheckRoutes
import syntax.http4sService._
import syntax.responseVerification._
import zio.Task
import zio.interop.catz._

final class HealthCheckHttpApiTests extends AnyFlatSpec with Matchers with Fixtures {

  implicit def testEncoder[A: Encoder] = jsonEncoderOf[Task, A]
  implicit def testDecoder[A: Decoder] = jsonOf[Task, A]

  it should "respond with Ok status 200 and the correct service signature" in {

    val httpApi: HttpRoutes[Task] =
      HealthCheckRoutes[Task].make(testLog)

    val verified = httpApi
      .runFor(Request[Task]())
      .verify[ServiceSignature](
        Status.Ok,
        sign =>
          (
            sign.name isSameAs "http4s-poc-api",
            sign.version isNotSameAs "",
            sign.scalaVersion isSameAs "2.12.8",
            sign.scalaOrganization isSameAs "org.scala-lang"
          ).mapN((_, _, _, _) => sign)
      )

    assertOn(verified)
  }
}
