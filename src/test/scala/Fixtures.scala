import java.util.concurrent.ForkJoinPool

import cats.{ Eq, Semigroup }
import cats.effect.IO
import cats.effect.laws.util.{ TestContext, TestInstances }
import cats.effect.util.CompositeException
import cats.tests.TestSettings
import external.library.syntax.ioAdapt._
import log.effect.fs2.SyncLogWriter.consoleLog
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.scalatest.check.ScalaCheckDrivenPropertyChecks
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{ Matchers, Succeeded }
import org.typelevel.discipline.scalatest.Discipline
import scalaz.concurrent.{ Task => ScalazTask }
import syntax.Verified

import scala.concurrent.ExecutionContext

trait Fixtures extends Matchers with Http4sDsl[IO] with Http4sClientDsl[IO] {

  implicit val C            = ExecutionContext.fromExecutor(new ForkJoinPool())
  implicit val timer        = IO.timer(C)
  implicit val contextShift = IO.contextShift(C)

  val testLog = consoleLog[IO]

  def assertOn[A](v: Verified[A]) =
    v.fold(es => es map { fail(_) }, _ => Succeeded)
}

abstract class MinimalSuite
    extends AnyFunSuite
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with Discipline
    with TestSettings
    with TestInstances {

  implicit val TC           = TestContext()
  implicit val C            = ExecutionContext.fromExecutor(new ForkJoinPool())
  implicit val timer        = IO.timer(C)
  implicit val contextShift = IO.contextShift(C)

  implicit def throwableSemigroup: Semigroup[Throwable] =
    new Semigroup[Throwable] {
      def combine(x: Throwable, y: Throwable): Throwable =
        CompositeException(x, y, Nil)
    }

  implicit def taskEq[A](implicit ev: Eq[IO[A]]): Eq[ScalazTask[A]] =
    new Eq[ScalazTask[A]] {
      def eqv(x: ScalazTask[A], y: ScalazTask[A]): Boolean =
        ev.eqv(x.as[IO], y.as[IO])
    }
}
