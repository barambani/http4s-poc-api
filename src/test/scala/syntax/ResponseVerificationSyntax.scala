package syntax

import java.nio.charset.StandardCharsets

import cats.data.Validated
import cats.effect.IO
import cats.instances.string._
import cats.syntax.eq._
import cats.syntax.show._
import cats.syntax.validated._
import cats.{ Eq, Show }
import org.http4s.{ EntityDecoder, Response, Status }

import scala.language.implicitConversions
import scala.util.Right

private[syntax] trait ResponseVerificationSyntax {

  implicit def verifiedSyntax[A](a: A): VerifiedOps[A]                     = new VerifiedOps(a)
  implicit def verifiedOptionSyntax[A](a: Option[A]): VerifiedOptionOps[A] = new VerifiedOptionOps(a)

  implicit def responseVerificationSyntax(response: IO[Response[IO]]) =
    new IoResponseResultOps(response)
}

private[syntax] class IoResponseResultOps(private val response: IO[Response[IO]]) extends AnyVal {

  import syntax.responseVerification._

  def verify[A: EntityDecoder[IO, ?]](status: Status, check: A => Verified[A])(
    implicit
    ev1: Eq[Status],
    ev2: Show[Status]
  ): Verified[A] =
    response.attempt
      .unsafeRunSync()
      .fold(
        err => s"Should succeed but returned the error $err".invalidNel,
        res =>
          res.status isSameAs status andThen { _ =>
            verifiedResponse[A](res, check)
        }
      )

  def verifyResponseText(status: Status, expected: String)(
    implicit
    ev1: Eq[Status],
    ev2: Show[Status]
  ): Verified[String] =
    response.attempt
      .unsafeRunSync()
      .fold(
        err => s"Should succeed but returned the error $err".invalidNel,
        res =>
          res.status isSameAs status andThen { _ =>
            verifiedResponseText(res, expected)
        }
      )

  private def verifiedResponse[A: EntityDecoder[IO, ?]](
    res: Response[IO],
    check: A => Verified[A]
  ): Verified[A] =
    res
      .as[A]
      .attempt
      .unsafeRunSync()
      .fold(
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

private[syntax] class VerifiedOps[A](private val a: A) extends AnyVal {

  def isNotSameAs(expected: =>A)(implicit ev1: Eq[A], ev2: Show[A]): Verified[A] =
    Validated.condNel(
      a =!= expected,
      a,
      s"Unexpected value. Expected different from ${expected.show} but was ${a.show}"
    )

  def isSameAs(expected: =>A)(implicit ev1: Eq[A], ev2: Show[A]): Verified[A] =
    Validated.condNel(a === expected, a, s"Unexpected value. Expected ${expected.show} but was ${a.show}")

  def is(p: A => Boolean, reason: =>String = "")(implicit ev: Show[A]): Verified[A] =
    Validated.condNel(p(a), a, s"Unexpected value ${a.show}: Reason $reason")
}

private[syntax] class VerifiedOptionOps[A](private val a: Option[A]) extends AnyVal {

  def isNotEmpty: Verified[Option[A]] =
    Validated.condNel(a.isDefined, a, s"Unexpected empty option value")
}
