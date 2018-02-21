import cats.effect.IO
import http4s.extend.syntax.Verified
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.scalatest.{Matchers, Succeeded}

trait Fixtures extends Matchers {

  object EitherHttp4sDsl      extends Http4sDsl[IO]
  object EitherHtt4sClientDsl extends Http4sClientDsl[IO]

  def assertOn[A](v: Verified[A]) =
    v.fold(es => es map { fail(_) }, _ => Succeeded)
}
