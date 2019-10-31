package typeclasses

import cats.effect.IO
import _root_.zio.{ Runtime, ZIO }

abstract class RunSync[F[_]] {
  def syncUnsafe[A](fa: F[A]): Either[Throwable, A]
}

object RunSync {
  @inline def apply[F[_]: RunSync]: RunSync[F] = implicitly

  implicit val catsIoRunSync: RunSync[IO] =
    new RunSync[IO] {
      def syncUnsafe[A](fa: IO[A]): Either[Throwable, A] =
        fa.attempt.unsafeRunSync()
    }

  implicit def zioRunSync[R](implicit rt: Runtime[R]): RunSync[ZIO[R, Throwable, *]] =
    new RunSync[ZIO[R, Throwable, *]] {
      def syncUnsafe[A](fa: ZIO[R, Throwable, A]): Either[Throwable, A] =
        rt.unsafeRunSync(fa).toEither
    }
}
