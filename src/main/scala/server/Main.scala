package server

import java.util.concurrent.ForkJoinPool

import cats.effect.{ ExitCode, IO, IOApp }
import cats.syntax.flatMap._
import cats.syntax.functor._
import integration.{ CacheIntegration, ProductIntegration, UserIntegration }
import io.circe.generic.auto._
import log.effect.fs2.SyncLogWriter._
import model.DomainModel._
import monix.execution.Scheduler
import org.http4s.circe._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import org.http4s.{ EntityDecoder, EntityEncoder, HttpApp }
import service.PriceService
import model.DomainModelCodecs._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends IOApp {

  implicit val futureExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new ForkJoinPool())

  implicit val monixTaskScheduler: Scheduler =
    Scheduler.global

  /**
    * encoding / decoding
    */
  implicit val priceRequestPayloadDecoder: EntityDecoder[IO, PricesRequestPayload] =
    jsonOf[IO, PricesRequestPayload]

  implicit val priceResponsePayloadEncoder: EntityEncoder[IO, List[Price]] =
    jsonEncoderOf[IO, List[Price]]

  implicit val healthCheckResponsePayloadEncoder: EntityEncoder[IO, ServiceSignature] =
    jsonEncoderOf[IO, ServiceSignature]

  /**
    * services
    */
  private[this] val priceService: IO[PriceService[IO]] =
    log4sLog[IO]("a logger") map { log =>
      PriceService(
        CacheIntegration[IO],
        UserIntegration[IO],
        ProductIntegration[IO],
        log,
        productTimeout = 10.seconds,
        preferenceTimeout = 10.seconds,
        priceTimeout = 10.seconds
      )
    }

  /**
    * routes
    */
  private[this] val routes: IO[HttpApp[IO]] =
    priceService map { ps =>
      Router(
        "/pricing-api/prices"       -> PriceHttpApi[IO].service(ps),
        "/pricing-api/health-check" -> HealthCheckHttpApi[IO].service()
      ).orNotFound
    }

  def run(args: List[String]): IO[ExitCode] =
    routes >>= { rs =>
      BlazeServerBuilder[IO]
        .enableHttp2(true)
        .withHttpApp(rs)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
}
