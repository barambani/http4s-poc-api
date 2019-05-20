package server

import cats.MonadError
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.ServiceError
import http4s.extend.ErrorResponse
import model.DomainModel._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityDecoder, EntityEncoder, HttpService, Method, Request, Response }
import service.PriceService

sealed abstract class PriceHttpApi[F[_]](
  implicit
  ME: MonadError[F, ServiceError],
  RD: EntityDecoder[F, PricesRequestPayload],
  RE: EntityEncoder[F, List[Price]],
  ER: ErrorResponse[F, ServiceError]
) extends Http4sDsl[F] {

  def service(priceService: PriceService[F]): HttpService[F] =
    HttpService[F] {
      case req @ Method.POST -> Root => postResponse(req, priceService) handleErrorWith ER.responseFor
    }

  private def postResponse(request: Request[F], priceService: PriceService[F]): F[Response[F]] =
    for {
      payload <- request.as[PricesRequestPayload]
      prices  <- priceService.prices(payload.userId, payload.productIds)
      resp    <- Ok(prices)
    } yield resp
}

object PriceHttpApi {
  def apply[F[_]](
    implicit
    ME: MonadError[F, ServiceError],
    RD: EntityDecoder[F, PricesRequestPayload],
    RE: EntityEncoder[F, List[Price]],
    ER: ErrorResponse[F, ServiceError]
  ): PriceHttpApi[F] =
    new PriceHttpApi[F] {}
}
