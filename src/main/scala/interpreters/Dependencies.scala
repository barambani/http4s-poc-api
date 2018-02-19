package interpreters

import cats.effect.IO
import errors.DependencyFailure
import external.{DummyTeamOneHttpApi, DummyTeamTwoHttpApi, TeamThreeCacheApi}
import http4s.extend.ExceptionDisplay._
import http4s.extend.syntax.byNameNt._
import http4s.extend.syntax.errorAdapt._
import http4s.extend.util.ThrowableModule._
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
      ecc: ExecutionContext,
      sch: Scheduler): Dependencies[IO] =
    new Dependencies[IO] {

      def user: UserId => IO[User] =
        id => DummyTeamTwoHttpApi.user(id)
          .attemptMapLeft[Throwable](
            // Translates the Throwable to the internal error system of the service. It could contain also the stack trace
            // or any relevant detail from the Throwable
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.user($id)", s"${ (unMk _ compose fullDisplay)(thr) }")
          )
          .liftIntoMonadError

      def usersPreferences: UserId => IO[UserPreferences] =
        id => DummyTeamOneHttpApi.usersPreferences(id)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.usersPreferences($id)", s"${ (unMk _ compose fullDisplay)(thr) }")
          )
          .liftIntoMonadError

      def product: ProductId => IO[Option[Product]] =
        ps => DummyTeamTwoHttpApi.product(ps)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamTwoHttpApi.products($ps)", s"${ (unMk _ compose fullDisplay)(thr) }")
          )
          .liftIntoMonadError

      def productPrice: Product => UserPreferences => IO[Price] =
        p => pref => DummyTeamOneHttpApi.productPrice(p)(pref)
          .attemptMapLeft[Throwable](
            thr => DependencyFailure(s"DummyTeamOneHttpApi.productPrice($p, $pref)", s"${ (unMk _ compose fullDisplay)(thr) }")
          )
          .liftIntoMonadError

      def cachedProduct: ProductId => IO[Option[Product]] =
        pId => TeamThreeCacheApi.get(pId).lift

      def storeProductToCache: ProductId => Product => IO[Unit] =
        pId => p => TeamThreeCacheApi.put(pId)(p).lift
    }
}