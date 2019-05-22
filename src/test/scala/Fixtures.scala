import cats.effect.IO
import cats.effect.laws.util.TestInstances
import cats.tests.TestSettings
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.scalatest.{ FunSuite, Matchers, Succeeded }
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.typelevel.discipline.scalatest.Discipline
import syntax.Verified

trait Fixtures extends Matchers {

  object EitherHttp4sDsl      extends Http4sDsl[IO]
  object EitherHtt4sClientDsl extends Http4sClientDsl[IO]

  def assertOn[A](v: Verified[A]) =
    v.fold(es => es map { fail(_) }, _ => Succeeded)
}

abstract class MinimalSuite
    extends FunSuite
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with Discipline
    with TestSettings
    with TestInstances
