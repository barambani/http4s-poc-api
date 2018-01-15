package lib.util

import cats.MonadError
import lib.ErrorInvariantMap

import scala.language.higherKinds

object MonadErrorModule {

  def adaptErrorType[F[_], E1, E2](me: MonadError[F, E1])(implicit EC: ErrorInvariantMap[E1, E2]): MonadError[F, E2] =
    new MonadError[F, E2] {

      def raiseError[A](e: E2): F[A] =
        (me.raiseError[A] _ compose EC.reverse)(e)

      def handleErrorWith[A](fa: F[A])(f: E2 => F[A]): F[A] =
        me.handleErrorWith(fa)(f compose EC.direct)

      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        me.flatMap(fa)(f)

      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
        me.tailRecM(a)(f)

      def pure[A](x: A): F[A] =
        me.pure(x)
    }
}