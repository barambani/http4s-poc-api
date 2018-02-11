package external

import cats.effect.IO

sealed trait LoggingApi {
  def error: String => IO[Unit]
  def warning: String => IO[Unit]
  def info: String => IO[Unit]
}

object LoggingApiImpl extends LoggingApi {
  def error: String => IO[Unit] = ???
  def warning: String => IO[Unit] = ???
  def info: String => IO[Unit] = ???
  def debug: String => IO[Unit] = ???
}
