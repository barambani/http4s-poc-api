package lib.instances

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.flatMap._
import lib.ErrorInvariantMap

object SyncInstances {

  implicit def eitherSync[Err](implicit EC: ErrorInvariantMap[Throwable, Err]): Sync[Either[Err, ?]] =
    new Sync[Either[Err, ?]] {

      def suspend[A](thunk: => Either[Err, A]): Either[Err, A] =
        thunk

      def pure[A](x: A): Either[Err, A] =
        x.asRight[Err]

      def flatMap[A, B](fa: Either[Err, A])(f: A => Either[Err, B]): Either[Err, B] =
        fa flatMap f

      def tailRecM[A, B](a: A)(f: A => Either[Err, Either[A, B]]): Either[Err, B] =
        a tailRecM f

      def raiseError[A](e: Throwable): Either[Err, A] =
        e.asLeft[A] leftMap EC.direct

      def handleErrorWith[A](fa: Either[Err, A])(f: Throwable => Either[Err, A]): Either[Err, A] =
        fa.fold(
          f compose EC.reverse,
          _.asRight[Err]
        )
    }
}