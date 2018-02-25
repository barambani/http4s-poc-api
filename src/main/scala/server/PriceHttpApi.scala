package server

import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{MonadError, Show}
import http4s.extend.ErrorResponse
import model.DomainModel._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpService, Method, Request, Response}
import service.PriceService

sealed abstract class PriceHttpApi[F[_], G[_]](
  implicit
    ME: MonadError[F, Throwable],
    RD: EntityDecoder[F, PricesRequestPayload],
    RE: EntityEncoder[F, List[Price]],
    TS: Show[Throwable],
    TR: ErrorResponse[F, Throwable]) extends Http4sDsl[F] {

  def service(priceService: PriceService[F, G]): HttpService[F] =
    HttpService[F] {
      case req @ Method.POST -> Root => postResponse(req, priceService) handleErrorWith TR.responseFor
    }

  private def postResponse(request: Request[F], priceService: PriceService[F, G]): F[Response[F]] =
    for {
      payload <- request.as[PricesRequestPayload]
      prices  <- priceService.prices(payload.userId, payload.productIds)
      resp    <- Ok(prices)
    } yield resp
}

object PriceHttpApi {
  def apply[F[_], G[_]](
    implicit
      ME: MonadError[F, Throwable],
      RD: EntityDecoder[F, PricesRequestPayload],
      RE: EntityEncoder[F, List[Price]],
      TS: Show[Throwable],
      TR: ErrorResponse[F, Throwable]): PriceHttpApi[F, G] =
    new PriceHttpApi[F, G]{}
}