package interpreters

import java.util.Calendar

import cats.effect.IO

object TestLogger {

  def testLogger: Logger[IO] =
    new Logger[IO] {
      def error: String => IO[Unit] =
        m => IO(println(s"${Calendar.getInstance().getTime} - [Thread ${ Thread.currentThread().getId }] - Test Log: Error --> $m"))

      def warning: String => IO[Unit] =
        m => IO(println(s"${Calendar.getInstance().getTime} - [Thread ${ Thread.currentThread().getId }] - Test Log: Warning --> $m"))

      def info: String => IO[Unit] =
        m => IO(println(s"${Calendar.getInstance().getTime} - [Thread ${ Thread.currentThread().getId }] - Test Log: Info --> $m"))

      def debug: String => IO[Unit] =
        m => IO(println(s"${Calendar.getInstance().getTime} - [Thread ${ Thread.currentThread().getId }] - Test Log: Debug --> $m"))
    }
}
