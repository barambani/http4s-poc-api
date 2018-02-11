package instances

import cats.Show
import org.http4s.Response
import org.http4s.dsl.Http4sDsl

trait ErrorResponse[F[_], E] extends Http4sDsl[F] {

  val ev: Show[E]
  def responseFor: E => F[Response[F]]
}

object ErrorResponse {

  @inline def apply[F[_], E](implicit inst: ErrorResponse[F, E]): ErrorResponse[F, E] = inst

  def defineFor[F[_], E : Show](f: String => F[Response[F]]): ErrorResponse[F, E] =
    new ErrorResponse[F, E] {
      val ev: Show[E] = Show[E]
      def responseFor: E => F[Response[F]] = f compose ev.show
    }
}
