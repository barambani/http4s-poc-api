package server

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import errors.PriceServiceError
import errors.PriceServiceError._
import external.library.syntax.response._
import model.DomainModel._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Method, Request, Response}
import service.PriceService

sealed abstract class PriceRoutes[F[_]: Sync](
  implicit requestDecoder: EntityDecoder[F, PricesRequestPayload],
  responseEncoder: EntityEncoder[F, List[Price]]
) extends Http4sDsl[F] {
  def make(priceService: PriceService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ Method.POST -> Root =>
      postResponse(req, priceService) handlingFailures priceServiceErrors handleErrorWith unhandledThrowable
    }

  private[this] def postResponse(request: Request[F], priceService: PriceService[F]): F[Response[F]] =
    for {
      payload <- request.as[PricesRequestPayload]
      prices  <- priceService.prices(payload.userId, payload.productIds)
      resp    <- Ok(prices)
    } yield resp

  private[this] def priceServiceErrors: PriceServiceError => F[Response[F]] = {
    case UserErr(r)                => FailedDependency(r)
    case PreferenceErr(r)          => FailedDependency(r)
    case ProductErr(r)             => FailedDependency(r)
    case ProductPriceErr(r)        => FailedDependency(r)
    case CacheLookupError(r)       => FailedDependency(r)
    case CacheStoreError(r)        => FailedDependency(r)
    case InvalidShippingCountry(r) => BadRequest(r)
  }

  private[this] def unhandledThrowable: Throwable => F[Response[F]] = { th =>
    import external.library.instances.throwable._
    InternalServerError(th.show)
  }
}

object PriceRoutes {
  def apply[
    F[_]: Sync: EntityDecoder[*[_], PricesRequestPayload]: EntityEncoder[*[_], List[Price]]
  ]: PriceRoutes[F] =
    new PriceRoutes[F] {}
}
