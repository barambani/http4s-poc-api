package server

import java.util.concurrent.ForkJoinPool

import cats.effect.IO
import fs2.StreamApp
import instances.ErrorConversionInstances._
import instances.MonadErrorInstances._
import io.circe.generic.auto._
import model.DomainModel._
import monix.execution.Scheduler
import org.http4s.circe._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.{EntityDecoder, EntityEncoder}
import service.{Dependencies, Logger, PriceService}

import scala.concurrent.ExecutionContext
import codecs.ServiceCodec._

object Main extends StreamApp[IO] {

  implicit val futureExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new ForkJoinPool())

  implicit val monixTaskScheduler: Scheduler =
    Scheduler.global

  /**
    * encoding / decoding
    */
  implicit val priceRequestPayloadDecoder: EntityDecoder[IO, PricesRequestPayload] =
    jsonOf[IO, PricesRequestPayload]

  implicit val priceResponsePayloadEncoder: EntityEncoder[IO, Seq[Price]] =
    jsonEncoderOf[IO, Seq[Price]]

  implicit val healthCheckResponsePayloadEncoder: EntityEncoder[IO, ServiceSignature] =
    jsonEncoderOf[IO, ServiceSignature]

  /**
    * server
    */
  def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    BlazeBuilder[IO]
      .mountService(HealthCheckHttpApi[IO].service(), "/pricing-api/health-check")
      .mountService(PriceHttpApi[IO].service(priceService), "/pricing-api/prices")
      .enableHttp2(true)
      .serve

  private def priceService: PriceService[IO] =
    PriceService(Dependencies[IO], Logger[IO])
}