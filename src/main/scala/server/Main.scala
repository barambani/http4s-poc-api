package server

import java.util.concurrent.ForkJoinPool

import external.{ TeamOneHttpApi, TeamThreeCacheApi, TeamTwoHttpApi }
import io.circe.generic.auto._
import log.effect.zio.ZioLogWriter._
import model.DomainModel._
import org.http4s.circe._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import org.http4s.{ EntityDecoder, EntityEncoder, HttpApp }
import service.PriceService
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{ Task, TaskR, ZIO }

import scala.concurrent.ExecutionContext
import model.DomainModelCodecs._

object Main extends CatsApp with RuntimePools with Encoding {

  private[this] val priceService: TaskR[String, PriceService[Task]] =
    log4sFromName map { log =>
      PriceService[Task](
        TeamThreeCacheApi.productCache,
        TeamOneHttpApi(),
        TeamTwoHttpApi(),
        log
      )
    }

  private[this] val httpApp: TaskR[String, HttpApp[Task]] =
    priceService map { ps =>
      Router(
        "/pricing-api/prices"       -> PriceHttpApi[Task].service(ps),
        "/pricing-api/health-check" -> HealthCheckHttpApi[Task].service(ps.logger)
      ).orNotFound
    }

  def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    (httpApp.provide("App log") >>= { app =>
      BlazeServerBuilder[Task]
        .bindHttp(17171, "localhost")
        .withConnectorPoolSize(64)
        .enableHttp2(true)
        .withHttpApp(app)
        .serve
        .compile
        .drain
    }).fold(_ => 0, _ => 1)
}

sealed trait RuntimePools {

  implicit val futureExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new ForkJoinPool())
}

sealed trait Encoding {

  implicit val priceRequestPayloadDecoder: EntityDecoder[Task, PricesRequestPayload] =
    jsonOf[Task, PricesRequestPayload]

  implicit val priceResponsePayloadEncoder: EntityEncoder[Task, List[Price]] =
    jsonEncoderOf[Task, List[Price]]

  implicit val healthCheckResponsePayloadEncoder: EntityEncoder[Task, ServiceSignature] =
    jsonEncoderOf[Task, ServiceSignature]
}
