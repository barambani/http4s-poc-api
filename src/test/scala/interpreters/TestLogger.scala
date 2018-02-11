package interpreters

import cats.MonadError
import cats.syntax.either._
import errors.ApiError

object TestLogger {

  def testLogger(implicit err: MonadError[Either[ApiError, ?], ApiError]): Logger[Either[ApiError, ?]] =
    new Logger[Either[ApiError, ?]] {
      def error: String => Either[ApiError, Unit]   = m => { println(s"Test Log: Error --> $m"); ().asRight }
      def warning: String => Either[ApiError, Unit] = m => { println(s"Test Log: Warning --> $m"); ().asRight }
      def info: String => Either[ApiError, Unit]    = m => { println(s"Test Log: Info --> $m"); ().asRight }
      def debug: String => Either[ApiError, Unit]    = m => { println(s"Test Log: Debug --> $m"); ().asRight }
    }
}
