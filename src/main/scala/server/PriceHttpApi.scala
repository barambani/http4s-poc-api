package server

import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{MonadError, Show}
import errors._
import http4s.extend.ErrorResponse
import model.DomainModel._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpService, Method, Request, Response}
import service.PriceService

sealed abstract class PriceHttpApi[F[_]](
  implicit
    ME: MonadError[F, ApiError],
    RD: EntityDecoder[F, PricesRequestPayload],
    RE: EntityEncoder[F, Seq[Price]]) extends Http4sDsl[F] {

  def service(priceService: PriceService[F]): HttpService[F] =
    HttpService[F] {
      case req @ Method.POST -> Root => postResponse(req, priceService) handleErrorWith errorHandler
    }

  private def postResponse(request: Request[F], priceService: PriceService[F]): F[Response[F]] =
    for {
      payload <- request.as[PricesRequestPayload]
      prices  <- priceService.prices(payload.userId, payload.productIds)
      resp    <- Ok(prices)
    } yield resp

  private def errorHandler: ApiError => F[Response[F]] = {
    case e: InvalidParameters       => responseFor(e)
    case e: DependencyFailure       => responseFor(e)
    case e: InvalidShippingCountry  => responseFor(e)
    case e: UnknownFailure          => responseFor(e)
  }

  private def responseFor[E : Show](e: E)(implicit ev: ErrorResponse[F, E]): F[Response[F]] =
    ev.responseFor(e)
}

object PriceHttpApi {
  def apply[F[_]](
    implicit
      ME: MonadError[F, ApiError],
      RD: EntityDecoder[F, PricesRequestPayload],
      RE: EntityEncoder[F, Seq[Price]]): PriceHttpApi[F] =
    new PriceHttpApi[F]{}
}