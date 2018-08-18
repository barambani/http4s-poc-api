package interpreters

import java.text.SimpleDateFormat
import java.util.Calendar

import cats.effect.IO

object TestLogger {

  def testLogger: Logger[IO] =
    new Logger[IO] {
      def error: (=>String) => IO[Unit] =
        m => formattedTimestamp flatMap (t => IO(println(s"$t - [Thread ${ Thread.currentThread().getId }] - Test Log: Error --> $m")))

      def warning: (=>String) => IO[Unit] =
        m => formattedTimestamp flatMap (t => IO(println(s"$t - [Thread ${ Thread.currentThread().getId }] - Test Log: Warning --> $m")))

      def info: (=>String) => IO[Unit] =
        m => formattedTimestamp flatMap (t => IO(println(s"$t - [Thread ${ Thread.currentThread().getId }] - Test Log: Info --> $m")))

      def debug: (=>String) => IO[Unit] =
        m => formattedTimestamp flatMap (t => IO(println(s"$t - [Thread ${ Thread.currentThread().getId }] - Test Log: Debug --> $m")))

      private def formattedTimestamp: IO[String] =
        IO(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(Calendar.getInstance().getTime))
    }
}
