package external

import cats.effect.IO
import org.log4s._

sealed trait LoggingApi {
  def error: String => IO[Unit]
  def warning: String => IO[Unit]
  def info: String => IO[Unit]
  def debug: String => IO[Unit]
}

object LoggingApi {

  @inline def apply(): LoggingApi =
    new LoggingApi {

      val logger = getLogger("http4s-poc-api")

      def error: String => IO[Unit] =
        m => IO(logger.logger.error(m))

      def warning: String => IO[Unit] =
        m => IO(logger.logger.warn(m))

      def info: String => IO[Unit] =
        m => IO(logger.logger.info(m))

      def debug: String => IO[Unit] =
        m => IO(logger.logger.debug(m))
    }
}
