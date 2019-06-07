package server

import cats.effect.Sync
import cats.effect.util.CompositeException
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import errors.PriceServiceError
import errors.PriceServiceError._
import external.library.instances.throwable._
import external.library.syntax.response._
import model.DomainModel._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityDecoder, EntityEncoder, HttpRoutes, Method, Request, Response }
import service.PriceService

sealed abstract class PriceHttpApi[F[_]: Sync](
  implicit
  RD: EntityDecoder[F, PricesRequestPayload],
  RE: EntityEncoder[F, List[Price]],
) extends Http4sDsl[F] {

  def service(priceService: PriceService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ Method.POST -> Root =>
        postResponse(req, priceService) handlingFailures priceServiceErrors handlingFailures compositeError
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
    case InvalidShippingCountry(r) => BadRequest(r)
    case CacheLookupError(r)       => BadGateway(r)
  }

  private[this] def compositeError: CompositeException => F[Response[F]] =
    e => BadGateway(e.show)
}

object PriceHttpApi {
  def apply[F[_]: Sync](
    implicit
    RD: EntityDecoder[F, PricesRequestPayload],
    RE: EntityEncoder[F, List[Price]]
  ): PriceHttpApi[F] =
    new PriceHttpApi[F] {}
}
