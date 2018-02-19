import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.instances.string._
import http4s.extend.syntax.Verified
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, Response, Status}
import org.scalatest.{Matchers, Succeeded}

import scala.util.Right

trait Fixtures extends Matchers {

  object EitherHttp4sDsl      extends Http4sDsl[IO]
  object EitherHtt4sClientDsl extends Http4sClientDsl[IO]

  def assertOn[A](v: Verified[A]) =
    v.fold(es => es map { fail(_) }, _ => Succeeded)

  implicit final class IoResponseResultOps(response: IO[Response[IO]]) {

    import cats.syntax.validated._
    import http4s.extend.syntax.responseVerification._
    import org.http4s.Http4s._

    def verify[A : EntityDecoder[IO, ?]](status: Status, check: A => Verified[A]): Verified[A] =
      response.attempt.unsafeRunSync().fold(
        err => s"Should succeed but returned the error $err".invalidNel,
        res => res.status isSameAs status andThen {
          _ => verifiedResponse[A](res, check)
        }
      )

    def verifyResponseText(status: Status, expected: String): Verified[String] =
      response.attempt.unsafeRunSync().fold(
        err => s"Should succeed but returned the error $err".invalidNel,
        res => res.status isSameAs status andThen {
          _ => verifiedResponseText(res, expected)
        }
      )

    private def verifiedResponse[A : EntityDecoder[IO, ?]](res: Response[IO], check: A => Verified[A]): Verified[A] =
      res.as[A].attempt.unsafeRunSync().fold(
        respErr => s"Response should succeed but returned the error $respErr".invalidNel,
        respRes => check(respRes)
      )

    private def verifiedResponseText[A](res: Response[IO], expected: String): Verified[String] =
      (res.body.compile.toVector.attempt.unsafeRunSync() match {
        case Right(b) => Right(b.toArray)
        case Left(e)  => Left(e)
      }).fold(
        respErr => s"Response should succeed but returned the error $respErr".invalidNel,
        respMsg => new String(respMsg, StandardCharsets.UTF_8) isSameAs expected
      )
  }
}
