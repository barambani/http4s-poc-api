package interpreters

import cats.effect.IO
import external.LoggingApi

trait Logger[F[_]] {
  def error: (=>String) => F[Unit]
  def warning: (=>String) => F[Unit]
  def info: (=>String) => F[Unit]
  def debug: (=>String) => F[Unit]
}

object Logger {

  @inline def apply[F[_]](implicit F: Logger[F]): Logger[F] = F

  implicit val ioLogger: Logger[IO] =
    new Logger[IO] {

      private val api = LoggingApi()

      def error: (=>String) => IO[Unit] =
        api.error

      def warning: (=>String) => IO[Unit] =
        api.warning

      def info: (=>String) => IO[Unit] =
        api.info

      def debug: (=>String) => IO[Unit] =
        api.debug
    }
}
