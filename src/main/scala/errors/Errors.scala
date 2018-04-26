package errors

import cats.{Monad, MonadError, Show}
import http4s.extend._
import org.http4s.Response

private[errors] sealed class MkThrowable extends NewType {
  def apply(b: Throwable): T = b.asInstanceOf[T]
  def mkF[F[_]](fs: F[Throwable]): F[T] = fs.asInstanceOf[F[T]]
}
private[errors] object MkThrowable {
  implicit final class MkThrowableOps(val `this`: MkThrowable#T) extends AnyVal {
    def unMk: Throwable = `this`.asInstanceOf[Throwable]
  }
}

object MkInvalidShippingCountry extends MkThrowable with InvalidShippingCountryInstances

private[errors] sealed trait InvalidShippingCountryInstances {

  implicit val invalidShippingShow: Show[InvalidShippingCountry] =
    new Show[InvalidShippingCountry] {
      def show(t: InvalidShippingCountry): String =
        s"InvalidShippingCountry: ${t.unMk.getMessage}"
    }

  implicit def invalidShippingCountryResponse[F[_] : Monad]: ErrorResponse[F, InvalidShippingCountry] =
    new ErrorResponse[F, InvalidShippingCountry] {
      val ev = Show[InvalidShippingCountry]
      def responseFor: InvalidShippingCountry => F[Response[F]] =
        e => InternalServerError(ev.show(e))
    }

  implicit def invalidShippingCountryMonadError[F[_]](implicit F: MonadError[F, Throwable]): MonadError[F, InvalidShippingCountry] =
    new MonadError[F, InvalidShippingCountry] {
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        F.flatMap(fa)(f)

      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
        F.tailRecM(a)(f)

      def raiseError[A](e: InvalidShippingCountry): F[A] =
        F.raiseError(e.unMk)

      def handleErrorWith[A](fa: F[A])(f: InvalidShippingCountry => F[A]): F[A] =
        F.handleErrorWith(fa)(f compose InvalidShippingCountry.apply)

      def pure[A](x: A): F[A] =
        F.pure(x)
    }
}

object MkDependencyFailure extends MkThrowable with DependencyFailureInstances

private[errors] sealed trait DependencyFailureInstances {

  implicit val dependencyFailureShow: Show[DependencyFailure] =
    new Show[DependencyFailure] {
      def show(t: DependencyFailure): String =
        s"DependencyFailure: ${t.unMk.getMessage}"
    }

  implicit def dependencyFailureResponse[F[_] : Monad]: ErrorResponse[F, DependencyFailure] =
    new ErrorResponse[F, DependencyFailure] {
      val ev = Show[DependencyFailure]
      def responseFor: DependencyFailure => F[Response[F]] =
        e => BadGateway(ev.show(e))
    }

  implicit def dependencyFailureMonadError[F[_]](implicit F: MonadError[F, Throwable]): MonadError[F, DependencyFailure] =
    new MonadError[F, DependencyFailure] {
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        F.flatMap(fa)(f)

      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
        F.tailRecM(a)(f)

      def raiseError[A](e: DependencyFailure): F[A] =
        F.raiseError(e.unMk)

      def handleErrorWith[A](fa: F[A])(f: DependencyFailure => F[A]): F[A] =
        F.handleErrorWith(fa)(f compose DependencyFailure.apply)

      def pure[A](x: A): F[A] =
        F.pure(x)
    }
}