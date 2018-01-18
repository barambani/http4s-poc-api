package server

import java.time.Instant

import cats.MonadError
import errors.ApiError
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpService, Method}
import model.DomainModel._

import scala.language.higherKinds

sealed abstract class HealthCheckHttpApi[F[_]](
  implicit
    ME: MonadError[F, ApiError],
    RE: EntityEncoder[F, ServiceSignature]) extends Http4sDsl[F] {

  def service(): HttpService[F] =
    HttpService[F] {
      case Method.GET -> Root => Ok(serviceSignature)
    }

  private val serviceSignature =
    ServiceSignature(
      name      = "",
      version   = "",
      buildTime = Instant.now()
    )
}

object HealthCheckHttpApi {
  def apply[F[_]](
    implicit
      ME: MonadError[F, ApiError],
      RE: EntityEncoder[F, ServiceSignature]): HealthCheckHttpApi[F] =
    new HealthCheckHttpApi[F]{}
}