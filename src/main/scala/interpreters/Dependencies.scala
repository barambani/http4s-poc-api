package interpreters

import cats.MonadError
import cats.effect.IO
import cats.syntax.apply._
import errors.{DependencyFailure, ServiceError}
import external.TeamThreeCacheApi._
import external._
import http4s.extend.syntax.byNameNt._
import http4s.extend.syntax.errorAdapt._
import model.DomainModel._
import monix.execution.Scheduler

trait Dependencies[F[_]] {
  def user: UserId => F[User]
  def usersPreferences: UserId => F[UserPreferences]
  def product: ProductId => F[Option[Product]]
  def productPrice: Product => UserPreferences => F[Price]
  def cachedProduct: ProductId => F[Option[Product]]
  def storeProductToCache: ProductId => Product => F[Unit]
}

object Dependencies {

  @inline def apply[F[_]](implicit F: Dependencies[F]): Dependencies[F] = F

  implicit def ioDependencies(
    implicit
      ev1: MonadError[IO, ServiceError],
      sc: Scheduler
  ): Dependencies[IO] =
    new Dependencies[IO] {

      def user: UserId => IO[User] =
        id => IO.shift *> TeamTwoHttpApi().user(id)
          .attemptMapLeft[ServiceError](
            // Translates the Throwable to the internal error system of the service. It could contain also the stack trace
            // or any relevant detail from the Throwable
            thr => DependencyFailure(thr).asServiceError
          )
          .liftIntoMonadError

      def usersPreferences: UserId => IO[UserPreferences] =
        id => IO.shift *> TeamOneHttpApi().usersPreferences(id)
          .attemptMapLeft[ServiceError](
            thr => DependencyFailure(thr).asServiceError
          )
          .liftIntoMonadError

      def product: ProductId => IO[Option[Product]] =
        ps => IO.shift *> TeamTwoHttpApi().product(ps)
          .attemptMapLeft[ServiceError](
            thr => DependencyFailure(thr).asServiceError
          )
          .liftIntoMonadError

      def productPrice: Product => UserPreferences => IO[Price] =
        p => pref => IO.shift *> TeamOneHttpApi().productPrice(p)(pref)
          .attemptMapLeft[ServiceError](
            thr => DependencyFailure(thr).asServiceError
          )
          .liftIntoMonadError

      def cachedProduct: ProductId => IO[Option[Product]] =
        pId => IO.shift *> TeamThreeCacheApi[ProductId, Product].get(pId).transformTo[IO]

      def storeProductToCache: ProductId => Product => IO[Unit] =
        pId => p => IO.shift *> TeamThreeCacheApi[ProductId, Product].put(pId)(p).transformTo[IO]
    }
}