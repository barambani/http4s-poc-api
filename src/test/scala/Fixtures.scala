import cats.MonadError
import cats.syntax.either._
import errors.ApiError
import http4s.extend.syntax.Verified
import model.DomainModel._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.scalatest.{Matchers, Succeeded}
import service.{Dependencies, Logger}

trait Fixtures extends Matchers {

  object EitherHttp4sDsl      extends Http4sDsl[Either[ApiError, ?]]
  object EitherHtt4sClientDsl extends Http4sClientDsl[Either[ApiError, ?]]

  def assertOn[A](v: Verified[A]) =
    v.fold(es => es map { fail(_) }, _ => Succeeded)

  def testSucceedingDependencies(
    aUser       : User,
    preferences : UserPreferences,
    aProduct    : Product,
    price       : Price)(
      implicit
        err: MonadError[Either[ApiError, ?], ApiError]): Dependencies[Either[ApiError, ?]] =
    new Dependencies[Either[ApiError, ?]] {
      def user                : UserId => Either[ApiError, User]                      = _ => aUser.asRight
      def usersPreferences    : UserId => Either[ApiError, UserPreferences]           = _ => preferences.asRight
      def product             : ProductId => Either[ApiError, Product]                = _ => aProduct.asRight
      def cachedProduct       : ProductId => Either[ApiError, Option[Product]]        = _ => None.asRight
      def productPrice        : Product => UserPreferences => Either[ApiError, Price] = _ => _ => price.asRight
      def storeProductToCache : ProductId => Product => Either[ApiError, Unit]        = _ => _ => ().asRight
    }

  def testLogger(implicit err: MonadError[Either[ApiError, ?], ApiError]): Logger[Either[ApiError, ?]] =
    new Logger[Either[ApiError, ?]] {
      def error: String => Either[ApiError, Unit]   = m => { println(s"Test Log: Error --> $m"); ().asRight }
      def warning: String => Either[ApiError, Unit] = m => { println(s"Test Log: Warning --> $m"); ().asRight }
      def info: String => Either[ApiError, Unit]    = m => { println(s"Test Log: Info --> $m"); ().asRight }
    }
}
