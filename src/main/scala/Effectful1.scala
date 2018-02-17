package temp

import cats.effect.{Effect, IO}
import cats.syntax.either._
import cats.{Apply, Semigroup, Semigroupal}
import errors.ApiError
import fs2.async
import cats.instances.either._

import scala.concurrent.ExecutionContext

trait Effectful1[F[_]] {

  val ev: Semigroupal[F]

  def runMapPar2[A, B, R](fa: F[A], fb: F[B])(f: (A, B) => R): F[R]

  def runMapPar3[A, B, C, R](fa: F[A], fb: F[B], fc: F[C])(f: (A, B, C) => R): F[R] =
    runMapPar2(fa, ev.product(fb, fc))((a, b) => f(a, b._1, b._2))

  def runPar2[A, B, R](fa: F[A], fb: F[B]): F[(A, B)] =
    runMapPar2(fa, fb)(Tuple2.apply)

  def runPar3[A, B, C, R](fa: F[A], fb: F[B], fc: F[C]): F[(A, B, C)] =
    runMapPar3(fa, fb, fc)(Tuple3.apply)
}

sealed trait Effectful1Instances {

  implicit def ioEffectful1(implicit ev: Effect[IO], ec: ExecutionContext): Effectful1[IO] =
    new Effectful1[IO] {

      val ev = Semigroupal[IO]
      val composedApply = Apply[IO] compose Apply[IO]

      def runMapPar2[A, B, R](fa: IO[A], fb: IO[B])(f: (A, B) => R): IO[R] =
        composedApply.map2(async.start(fa), async.start(fb))(f) flatMap identity
    }

  implicit def eitherEffectful1(implicit iev: Semigroup[ApiError]): Effectful1[Either[ApiError, ?]] =
    new Effectful1[Either[ApiError, ?]] {

      val ev = Semigroupal[Either[ApiError, ?]]

      def runMapPar2[A, B, R](fa: Either[ApiError, A], fb: Either[ApiError, B])(f: (A, B) => R): Either[ApiError, R] =
        (fa, fb) match {
          case (Right(a), Right(b)) => f(a, b).asRight
          case (Left(a) , Left(b))  => iev.combine(a, b).asLeft
          case (Left(a) , _)        => a.asLeft
          case (_       , Left(b))  => b.asLeft
        }
    }
}

object Effectful1 extends Effectful1Instances {
  @inline def apply[F[_]](implicit F: Effectful1[F]): Effectful1[F] = F
}