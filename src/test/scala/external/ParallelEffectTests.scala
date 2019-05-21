package external

import java.util.concurrent.ForkJoinPool

import cats.Show
import cats.effect.util.CompositeException
import cats.effect.{ Effect, IO, Timer }
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.show._
import external.library.ParallelEffect
import org.scalatest.{ Matchers, WordSpecLike }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, TimeoutException }

final class ParallelEffectTests extends WordSpecLike with Matchers {

  implicit val C = ExecutionContext.fromExecutor(new ForkJoinPool())

  def eff   = Effect[IO]
  def pEff  = ParallelEffect[IO]
  def timer = Timer[IO]

  val e1 = new Throwable("error 1")
  val e2 = new Throwable("error 2")

  def waiting(t: FiniteDuration) = timer.sleep(t)

  implicit def throwableShow[E <: Throwable]: Show[E] = {
    case s: CompositeException =>
      (s.head :: s.tail).map(th => s"Failed with exception $th").toList.mkString("\n")
    case t => s"Failed with exception $t"
  }

  "the parallel execution" should {

    "fail with composite exception when both fail waiting for both to finish" in {

      val parallel = pEff.parallelRun(
        waiting(200.milliseconds) *> eff.raiseError[Int](e1),
        waiting(100.milliseconds) *> eff.raiseError[Int](e2)
      )(1.second)

      parallel.attempt.unsafeRunSync().leftMap(_.show) should be(CompositeException(e1, e2, Nil).show.asLeft)
    }

    "fail with a timeout exception when one succeeds and one takes too long" in {

      val parallel = pEff.parallelRun(
        waiting(100.milliseconds) *> eff.delay(1),
        waiting(2.seconds) *> eff.delay(2)
      )(500.milliseconds)

      parallel.attempt.unsafeRunSync().leftMap(_.show) should be(
        new TimeoutException(s"${500.milliseconds}").show.asLeft
      )
    }

    "fail with a composite exception containing a timeout exception and the failure when one fails and one takes too long" in {

      val parallel = pEff.parallelRun(
        waiting(2.seconds) *> eff.delay(2),
        waiting(100.milliseconds) *> eff.raiseError[Int](e1)
      )(500.milliseconds)

      parallel.attempt.unsafeRunSync().leftMap(_.show) should be(
        CompositeException(new TimeoutException(s"${500.milliseconds}"), e1, Nil).show.asLeft
      )
    }

    "return the values when both succeed within a timeout" in {

      val parallel = pEff.parallelRun(
        waiting(200.milliseconds) *> eff.delay(2),
        waiting(100.milliseconds) *> eff.delay(3)
      )(500.milliseconds)

      parallel.attempt.unsafeRunSync() should be((2, 3).asRight)
    }
  }
}
