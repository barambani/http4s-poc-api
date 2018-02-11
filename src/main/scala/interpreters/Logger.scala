package interpreters

import cats.MonadError
import cats.effect.IO
import errors.ApiError
import external.LoggingApiImpl

trait Logger[F[_]] {
  def error: String => F[Unit]
  def warning: String => F[Unit]
  def info: String => F[Unit]
  def debug: String => F[Unit]
}

object Logger {

  @inline def apply[F[_]](implicit F: Logger[F]): Logger[F] = F

  implicit def ioLogger(implicit err: MonadError[IO, ApiError]): Logger[IO] =
    new Logger[IO] {
      def error: String => IO[Unit] =
        LoggingApiImpl.error

      def warning: String => IO[Unit] =
        LoggingApiImpl.warning

      def info: String => IO[Unit] =
        LoggingApiImpl.info

      def debug: String => IO[Unit] =
        LoggingApiImpl.debug
    }
}
