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

  implicit val execution: ExecutionContext =
    ExecutionContext.fromExecutor(new ForkJoinPool())

  implicit val scheduler: Scheduler =
    Scheduler.global

  /**
    * encoding / decoding
    */
  implicit val requestPayloadDecoder: EntityDecoder[IO, PricesRequestPayload] =
    jsonOf[IO, PricesRequestPayload]

  implicit val pricingPayloadEncoder: EntityEncoder[IO, Seq[Price]] =
    jsonEncoderOf[IO, Seq[Price]]

  implicit val serviceInfoPayloadEncoder: EntityEncoder[IO, ServiceSignature] =
    jsonEncoderOf[IO, ServiceSignature]

  /**
    * server
    */
  def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    BlazeBuilder[IO]
      .mountService(HealthCheckHttpApi[IO].service(), "/pricing-api/health-check")
      .mountService(PriceHttpApi[IO].service(pricingIo), "/pricing-api/prices")
      .serve

  private def pricingIo: PriceService[IO] =
    PriceService(Dependencies[IO], Logger[IO])
}