package lib.syntax

import cats.syntax.flatMap._
import cats.{FlatMap, Functor}
import org.http4s.{HttpService, Request, Response}

import scala.language.higherKinds

object Http4sServiceSyntax {

  implicit final class HttpServiceSyntaxOps[F[_]](service: HttpService[F]) {

    def runFor(req: Request[F])(implicit F: Functor[F]): F[Response[F]] =
      service.run(req).getOrElse(Response.notFound)

    def runForF(req: F[Request[F]])(implicit F: FlatMap[F]): F[Response[F]] =
      req flatMap (service.run(_).getOrElse(Response.notFound))
  }
}