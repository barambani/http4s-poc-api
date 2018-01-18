package service

import cats.MonadError
import cats.effect.IO
import dependencies.{DummyTeamOneHttpApi, DummyTeamTwoHttpApi, TeamThreeCacheApi}
import errors.{ApiError, DependencyFailure}
import http4s.extend.syntax.ByNameNaturalTransformationSyntax._
import http4s.extend.syntax.ErrorAdaptSyntax._
import model.DomainModel._
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

trait Dependencies[F[_]] {
  def user: UserId => F[User]
  def usersPreferences: UserId => F[UserPreferences]
  def product: ProductId => F[Product]
  def cachedProduct: ProductId => F[Option[Product]]
  def productPrice: Product => UserPreferences => F[Price]
  def storeProductToCache: ProductId => Product => F[Unit]
}

object Dependencies {

  def apply[F[_]](implicit F: Dependencies[F]): Dependencies[F] = F

  implicit def ioDependencies(implicit err: MonadError[IO, ApiError], ec: ExecutionContext, s: Scheduler): Dependencies[IO] =
    new Dependencies[IO] {

      def user: UserId => IO[User] =
        id => DummyTeamTwoHttpApi.user(id)
          .adaptError[ApiError](
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.user for the id $id", s"${thr.getMessage}")
          )
          .liftIntoMonadError

      def usersPreferences: UserId => IO[UserPreferences] =
        id => DummyTeamOneHttpApi.usersPreferences(id)
          .adaptError[ApiError](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.usersPreferences with parameter $id", s"${thr.getMessage}")
          )
          .liftIntoMonadError

      def product: ProductId => IO[Product] =
        ps => DummyTeamTwoHttpApi.product(ps)
          .adaptError[ApiError](
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.products for the ids $ps", s"${thr.getMessage}")
          )
          .liftIntoMonadError

      def cachedProduct: ProductId => IO[Option[Product]] =
        pId => TeamThreeCacheApi.get(pId).lift

      def storeProductToCache: ProductId => Product => IO[Unit] =
        pId => p => TeamThreeCacheApi.put(pId)(p).lift

      def productPrice: Product => UserPreferences => IO[Price] =
        p => pref => DummyTeamOneHttpApi.productPrice(p)(pref)
          .adaptError[ApiError](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.productPrice with parameters <$p> and <$pref>", s"${thr.getMessage}")
          )
          .liftIntoMonadError
    }
}