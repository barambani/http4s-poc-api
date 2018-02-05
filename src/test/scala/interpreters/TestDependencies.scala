package interpreters

import cats.MonadError
import cats.syntax.either._
import errors.{ApiError, DependencyFailure}
import model.DomainModel._

object TestDependencies {

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

  def testFailingDependencies(implicit err: MonadError[Either[ApiError, ?], ApiError]): Dependencies[Either[ApiError, ?]] =
    new Dependencies[Either[ApiError, ?]] {
      def user: UserId => Either[ApiError, User] =
        _ => DependencyFailure("def user: UserId => Either[ApiError, User]", "network failure").asLeft

      def usersPreferences: UserId => Either[ApiError, UserPreferences] =
        _ => DependencyFailure("def usersPreferences: UserId => Either[ApiError, UserPreferences]", "timeout").asLeft

      def product: ProductId => Either[ApiError, Product] =
        _ => DependencyFailure("def product: ProductId => Either[ApiError, Product]]", "network failure").asLeft

      def cachedProduct: ProductId => Either[ApiError, Option[Product]] =
        _ => DependencyFailure("def cachedProduct: ProductId => Either[ApiError, Option[Product]]", "not responding").asLeft

      def productPrice: Product => UserPreferences => Either[ApiError, Price] =
        _ => _ => DependencyFailure("def productPrice: Product => UserPreferences => Either[ApiError, Price]", "timeout").asLeft

      def storeProductToCache: ProductId => Product => Either[ApiError, Unit] =
        _ => _ => DependencyFailure("def storeProductToCache: ProductId => Product => Either[ApiError, Unit]", "not responding").asLeft
    }
}
