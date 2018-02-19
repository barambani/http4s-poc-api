package interpreters

import cats.effect.IO

object TestLogger {

  def testLogger: Logger[IO] =
    new Logger[IO] {
      def error: String => IO[Unit]   = m => IO { println(s"Test Log: Error --> $m") }
      def warning: String => IO[Unit] = m => IO { println(s"Test Log: Warning --> $m") }
      def info: String => IO[Unit]    = m => IO { println(s"Test Log: Info --> $m") }
      def debug: String => IO[Unit]   = m => IO { println(s"Test Log: Debug --> $m") }
    }
}
