package server

import cats.MonadError
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import errors.{ApiError, DependencyFailure, InvalidParameters, UnknownFailure}
import model.DomainModel$._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpService, Method, Request, Response}
import service.PriceService

import scala.language.higherKinds

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
    case e: InvalidParameters => BadRequest(e.toString)
    case e: DependencyFailure => BadGateway(e.toString)
    case e: UnknownFailure    => InternalServerError(e.toString)
  }
}

object PriceHttpApi {
  def apply[F[_]](
    implicit
      ME: MonadError[F, ApiError],
      RD: EntityDecoder[F, PricesRequestPayload],
      RE: EntityEncoder[F, Seq[Price]]): PriceHttpApi[F] =
    new PriceHttpApi[F]{}
}