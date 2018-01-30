package service

import cats.MonadError
import cats.effect.IO
import external.LoggingApiImpl
import errors.ApiError

trait Logger[F[_]] {
  def error: String => F[Unit]
  def warning: String => F[Unit]
  def info: String => F[Unit]
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
    }
}
