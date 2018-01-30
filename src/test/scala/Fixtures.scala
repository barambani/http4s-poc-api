import cats.MonadError
import errors.ApiError
import http4s.extend.syntax.Verified
import model.DomainModel
import model.DomainModel.{ProductId, UserId}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.scalatest.{Matchers, Succeeded}
import service.{Dependencies, Logger}
import cats.syntax.either._

trait Fixtures extends Matchers {

  object EitherHttp4sDsl      extends Http4sDsl[Either[ApiError, ?]]
  object EitherHtt4sClientDsl extends Http4sClientDsl[Either[ApiError, ?]]

  def assertOn[A](v: Verified[A]) =
    v.fold(es => es map { fail(_) }, _ => Succeeded)

  def testSucceedingDependencies(implicit err: MonadError[Either[ApiError, ?], ApiError]): Dependencies[Either[ApiError, ?]] =
    new Dependencies[Either[ApiError, ?]] {
      def user: UserId => Either[ApiError, DomainModel.User] = ???
      def usersPreferences: UserId => Either[ApiError, DomainModel.UserPreferences] = ???
      def product: ProductId => Either[ApiError, DomainModel.Product] = ???
      def cachedProduct: ProductId => Either[ApiError, Option[DomainModel.Product]] = ???
      def productPrice: DomainModel.Product => DomainModel.UserPreferences => Either[ApiError, DomainModel.Price] = ???
      def storeProductToCache: ProductId => DomainModel.Product => Either[ApiError, Unit] = ???
    }

  def testLogger(implicit err: MonadError[Either[ApiError, ?], ApiError]): Logger[Either[ApiError, ?]] =
    new Logger[Either[ApiError, ?]] {
      def error: String => Either[ApiError, Unit]   = _ => ().asRight
      def warning: String => Either[ApiError, Unit] = _ => ().asRight
      def info: String => Either[ApiError, Unit]    = _ => ().asRight
    }
}
