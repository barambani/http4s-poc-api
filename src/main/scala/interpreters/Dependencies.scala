package interpreters

import cats.Show
import cats.effect.IO
import cats.syntax.apply._
import errors.DependencyFailure
import external.TeamThreeCacheApi._
import external._
import http4s.extend.syntax.byNameNt._
import http4s.extend.syntax.errorAdapt._
import model.DomainModel._
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext

trait Dependencies[F[_]] {
  def user: UserId => F[User]
  def usersPreferences: UserId => F[UserPreferences]
  def product: ProductId => F[Option[Product]]
  def cachedProduct: ProductId => F[Option[Product]]
  def productPrice: Product => UserPreferences => F[Price]
  def storeProductToCache: ProductId => Product => F[Unit]
}

object Dependencies {

  @inline def apply[F[_]](implicit F: Dependencies[F]): Dependencies[F] = F

  implicit def ioDependencies(
    implicit
      ev: Show[Throwable],
      ec: ExecutionContext,
      sc: Scheduler): Dependencies[IO] =
    new Dependencies[IO] {

      def user: UserId => IO[User] =
        id => IO.shift *> TeamTwoHttpApi().user(id)
          .attemptMapLeft[Throwable](
            // Translates the Throwable to the internal error system of the service. It could contain also the stack trace
            // or any relevant detail from the Throwable
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.user($id)", s"${ ev.show(thr) }")
          )
          .liftIntoMonadError

      def usersPreferences: UserId => IO[UserPreferences] =
        id => IO.shift *> TeamOneHttpApi().usersPreferences(id)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.usersPreferences($id)", s"${ ev.show(thr) }")
          )
          .liftIntoMonadError

      def product: ProductId => IO[Option[Product]] =
        ps => IO.shift *> TeamTwoHttpApi().product(ps)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.products($ps)", s"${ ev.show(thr) }")
          )
          .liftIntoMonadError

      def productPrice: Product => UserPreferences => IO[Price] =
        p => pref => IO.shift *> TeamOneHttpApi().productPrice(p)(pref)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.productPrice($p, $pref)", s"${ ev.show(thr) }")
          )
          .liftIntoMonadError

      def cachedProduct: ProductId => IO[Option[Product]] =
        pId => IO.shift *> TeamThreeCacheApi[ProductId, Product].get(pId).transformTo[IO]

      def storeProductToCache: ProductId => Product => IO[Unit] =
        pId => p => IO.shift *> TeamThreeCacheApi[ProductId, Product].put(pId)(p).transformTo[IO]
    }
}