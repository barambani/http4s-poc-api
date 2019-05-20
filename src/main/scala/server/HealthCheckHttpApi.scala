package server

import cats.MonadError
import model.DomainModel._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityEncoder, HttpService, Method }

sealed abstract class HealthCheckHttpApi[F[_]](
  implicit
  ME: MonadError[F, Throwable],
  RE: EntityEncoder[F, ServiceSignature]
) extends Http4sDsl[F] {

  def service(): HttpService[F] =
    HttpService[F] {
      case Method.GET -> Root => Ok(serviceSignature)
    }

  private val serviceSignature =
    ServiceSignature(
      name = BuildInfo.name,
      version = BuildInfo.version,
      scalaVersion = BuildInfo.scalaVersion,
      scalaOrganization = BuildInfo.scalaOrganization,
      buildTime = BuildInfo.buildTime
    )
}

object HealthCheckHttpApi {
  def apply[F[_]](
    implicit
    ME: MonadError[F, Throwable],
    RE: EntityEncoder[F, ServiceSignature]
  ): HealthCheckHttpApi[F] =
    new HealthCheckHttpApi[F] {}
}
