import java.util.concurrent.ForkJoinPool

import cats.effect.IO
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.scalatest.Succeeded
import org.scalatest.matchers.should.Matchers
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
      override val platform =
        PlatformLive
          .makeDefault()
          .withTracingConfig(TracingConfig.disabled)
          .withReportFailure(_ => ())
    }

  def assertOn[A](v: Verified[A]) =
    v.fold(es => es map { fail(_) }, _ => Succeeded)
}
