package interpreters

import cats.MonadError
import cats.effect.IO
import errors.{ApiError, DependencyFailure}
import external.{DummyTeamOneHttpApi, DummyTeamTwoHttpApi, TeamThreeCacheApi}
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

  implicit def ioDependencies(implicit err: MonadError[IO, ApiError], ec: ExecutionContext, s: Scheduler): Dependencies[IO] =
    new Dependencies[IO] {

      def user: UserId => IO[User] =
        id => DummyTeamTwoHttpApi.user(id)
          .attemptMapLeft[ApiError](
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.user for the id $id", s"${thr.getMessage}")
          )
          .liftIntoMonadError

      def usersPreferences: UserId => IO[UserPreferences] =
        id => DummyTeamOneHttpApi.usersPreferences(id)
          .attemptMapLeft[ApiError](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.usersPreferences with parameter $id", s"${thr.getMessage}")
          )
          .liftIntoMonadError

      def product: ProductId => IO[Option[Product]] =
        ps => DummyTeamTwoHttpApi.product(ps)
          .attemptMapLeft[ApiError](
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.products for the ids $ps", s"${thr.getMessage}")
          )
          .liftIntoMonadError

      def cachedProduct: ProductId => IO[Option[Product]] =
        pId => TeamThreeCacheApi.get(pId).lift

      def storeProductToCache: ProductId => Product => IO[Unit] =
        pId => p => TeamThreeCacheApi.put(pId)(p).lift

      def productPrice: Product => UserPreferences => IO[Price] =
        p => pref => DummyTeamOneHttpApi.productPrice(p)(pref)
          .attemptMapLeft[ApiError](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.productPrice with parameters <$p> and <$pref>", s"${thr.getMessage}")
          )
          .liftIntoMonadError
    }
}