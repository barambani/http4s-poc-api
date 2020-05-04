package syntax

import cats.Functor
import cats.data.{Kleisli, OptionT}
import org.http4s.{HttpRoutes, Request, Response}

import scala.language.implicitConversions

private[syntax] trait Http4sServiceSyntax {
  implicit def httpServiceSyntax[F[_]](s: HttpRoutes[F]): HttpServiceOps[F] = new HttpServiceOps(s)
}

/**
  * Here the parameter's type needs to be explicitly de-aliased to
  * `Kleisli[OptionT[F, *], Request[F], Response[F]]` otherwise the
  * compilation will fail when the parameter is made non private
  */
private[syntax] class HttpServiceOps[F[_]](
  private val service: Kleisli[OptionT[F, *], Request[F], Response[F]]
) extends AnyVal {
  def runFor(req: Request[F])(implicit F: Functor[F]): F[Response[F]] =
    service.run(req).getOrElse(Response.notFound)
}
