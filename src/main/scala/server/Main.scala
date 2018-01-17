package server

import java.util.concurrent.ForkJoinPool

import cats.effect.IO
import fs2.StreamApp
import io.circe.generic.auto._
import model.DomainModel._
import org.http4s.circe._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.{EntityDecoder, EntityEncoder}
import service.{Dependencies, Logger, PriceService}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.higherKinds
import codecs.ServiceCodec._

object Main extends StreamApp[IO] {

  implicit val execution: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(new ForkJoinPool())

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
      .mountService(HealthCheckHttpApi[IO].service(), "/api-pricing/info")
      .mountService(PriceHttpApi[IO].service(pricingIo), "/api-pricing/pricing")
      .serve

  private def pricingIo: PriceService[IO] =
    PriceService(Dependencies[IO], Logger[IO])
}