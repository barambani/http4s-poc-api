package interpreters

import cats.Show
import cats.effect.IO
import errors.DependencyFailure
import external.{TeamTwoDbApi, _}
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

      lazy val teamOneHttpApi = TeamOneHttpApi()
      lazy val teamTwoDbApi = TeamTwoDbApi()
      lazy val teamThreeCacheApi = TeamThreeCacheApi()

      def user: UserId => IO[User] =
        id => teamTwoDbApi.user(id)
          .attemptMapLeft[Throwable](
            // Translates the Throwable to the internal error system of the service. It could contain also the stack trace
            // or any relevant detail from the Throwable
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.user($id)", s"${ ev.show(thr) }")
          )
          .liftIntoMonadError

      def usersPreferences: UserId => IO[UserPreferences] =
        id => teamOneHttpApi.usersPreferences(id)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.usersPreferences($id)", s"${ ev.show(thr) }")
          )
          .liftIntoMonadError

      def product: ProductId => IO[Option[Product]] =
        ps => teamTwoDbApi.product(ps)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.products($ps)", s"${ ev.show(thr) }")
          )
          .liftIntoMonadError

      def productPrice: Product => UserPreferences => IO[Price] =
        p => pref => teamOneHttpApi.productPrice(p)(pref)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.productPrice($p, $pref)", s"${ ev.show(thr) }")
          )
          .liftIntoMonadError

      def cachedProduct: ProductId => IO[Option[Product]] =
        pId => teamThreeCacheApi.get(pId).lift

      def storeProductToCache: ProductId => Product => IO[Unit] =
        pId => p => teamThreeCacheApi.put(pId)(p).lift
    }
}