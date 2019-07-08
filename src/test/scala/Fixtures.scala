import java.util.concurrent.ForkJoinPool

import cats.effect.IO
import log.effect.zio.ZioLogWriter.console
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.scalatest.{ Matchers, Succeeded }
import syntax.Verified
import zio.DefaultRuntime
import zio.internal.PlatformLive
import zio.internal.tracing.TracingConfig

import scala.concurrent.ExecutionContext

trait Fixtures extends Matchers with Http4sDsl[IO] with Http4sClientDsl[IO] {

  implicit val C            = ExecutionContext.fromExecutor(new ForkJoinPool())
  implicit val timer        = IO.timer(C)
  implicit val contextShift = IO.contextShift(C)

  implicit val testRuntime: DefaultRuntime =
    new DefaultRuntime {
      override val Platform =
        PlatformLive
          .makeDefault()
          .withTracingConfig(TracingConfig.disabled)
          .withReportFailure(_ => ())
    }

  val testLog = console

  def assertOn[A](v: Verified[A]) =
    v.fold(es => es map { fail(_) }, _ => Succeeded)
}
