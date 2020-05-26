package server

import java.util.concurrent.Executors

import com.github.ghik.silencer.silent
import external.{TeamOneHttpApi, TeamThreeCacheApi, TeamTwoHttpApi}
import io.circe.generic.auto._
import log.effect.zio.ZioLogWriter._
import model.DomainModel._
import org.http4s.circe._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import org.http4s.{EntityDecoder, EntityEncoder, HttpApp}
import service.PriceService
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{ExitCode, RIO, Task, ZEnv, ZIO}

import scala.concurrent.ExecutionContext
import model.DomainModelCodecs._

@silent
object Main extends zio.interop.catz.CatsApp with Pools with Codecs {
  private[this] val priceService: RIO[String, PriceService[Task]] =
    log4sFromName map { log =>
      PriceService[Task](
        TeamThreeCacheApi.productCache,
        TeamOneHttpApi(),
        TeamTwoHttpApi(),
        log
      )
    }

  private[this] val httpApp: RIO[PriceService[Task], HttpApp[Task]] =
    ZIO.access { ps =>
      Router(
        "/pricing-api/prices"       -> PriceRoutes[Task].make(ps),
        "/pricing-api/health-check" -> HealthCheckRoutes[Task].make(ps.logger)
      ).orNotFound
    }

  private[this] val runningServer: RIO[HttpApp[Task], Unit] =
    ZIO.accessM { app =>
      BlazeServerBuilder[Task](serverPool)
        .bindHttp(17171, "0.0.0.0")
        .withConnectorPoolSize(connectorPoolSize)
        .enableHttp2(true)
        .withHttpApp(app)
        .serve
        .compile
        .drain
    }

  private[this] val serviceRuntime: RIO[String, Unit] =
    priceService >>> httpApp >>> runningServer

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    serviceRuntime.fold(_ => ExitCode.failure, _ => ExitCode.success) provide "App log"
}

sealed trait Pools {

  protected val connectorPoolSize = Runtime.getRuntime.availableProcessors() * 2
  protected val mainThreadNumber  = Runtime.getRuntime.availableProcessors() + 1

  protected val serverPool = ExecutionContext.fromExecutor(
    Executors.newWorkStealingPool(mainThreadNumber)
  )
}

sealed trait Codecs {
  implicit val priceRequestPayloadDecoder: EntityDecoder[Task, PricesRequestPayload] =
    jsonOf[Task, PricesRequestPayload]

  implicit val priceResponsePayloadEncoder: EntityEncoder[Task, List[Price]] =
    jsonEncoderOf[Task, List[Price]]

  implicit val healthCheckResponsePayloadEncoder: EntityEncoder[Task, ServiceSignature] =
    jsonEncoderOf[Task, ServiceSignature]
}
