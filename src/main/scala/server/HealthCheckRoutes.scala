package server

import cats.effect.Sync
import log.effect.LogWriter
import model.DomainModel._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityEncoder, HttpRoutes, Method }
import cats.syntax.flatMap._

sealed abstract class HealthCheckRoutes[F[_]: Sync](
  implicit responseEncoder: EntityEncoder[F, ServiceSignature]
) extends Http4sDsl[F] {

  def make(log: LogWriter[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case Method.GET -> Root => log.debug(s"Serving HealthCheck request") >> Ok(serviceSignature)
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

object HealthCheckRoutes {
  def apply[F[_]: Sync: EntityEncoder[*[_], ServiceSignature]]: HealthCheckRoutes[F] =
    new HealthCheckRoutes[F] {}
}
