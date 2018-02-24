package interpreters

import cats.effect.IO
import external.LoggingApi

trait Logger[F[_]] {
  def error: String => F[Unit]
  def warning: String => F[Unit]
  def info: String => F[Unit]
  def debug: String => F[Unit]
}

object Logger {

  @inline def apply[F[_]](implicit F: Logger[F]): Logger[F] = F

  implicit def ioLogger: Logger[IO] =
    new Logger[IO] {
      def error: String => IO[Unit] =
        LoggingApi().error

      def warning: String => IO[Unit] =
        LoggingApi().warning

      def info: String => IO[Unit] =
        LoggingApi().info

      def debug: String => IO[Unit] =
        LoggingApi().debug
    }
}
