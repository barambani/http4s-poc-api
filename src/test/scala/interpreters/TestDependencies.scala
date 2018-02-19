package interpreters

import cats.effect.IO
import errors.DependencyFailure
import model.DomainModel._

object TestDependencies {

  def testSucceedingDependencies(
    aUser           : User,
    preferences     : UserPreferences,
    productsInStore : Map[ProductId, Product],
    productsInCache : Map[ProductId, Product],
    price           : Price): Dependencies[IO] =
      new Dependencies[IO] {
        def user                : UserId => IO[User]                      = _ => IO.pure(aUser)
        def usersPreferences    : UserId => IO[UserPreferences]           = _ => IO.pure(preferences)
        def product             : ProductId => IO[Option[Product]]        = id => IO.pure(productsInStore.get(id))
        def cachedProduct       : ProductId => IO[Option[Product]]        = id => IO.pure(productsInCache.get(id))
        def productPrice        : Product => UserPreferences => IO[Price] = _ => _ => IO.pure(price)
        def storeProductToCache : ProductId => Product => IO[Unit]        = _ => _ => IO.unit
      }

  def testFailingDependencies: Dependencies[IO] =
    new Dependencies[IO] {
      def user: UserId => IO[User] =
        _ => IO.raiseError(DependencyFailure("def user: UserId => IO[User]", "network failure"))

      def usersPreferences: UserId => IO[UserPreferences] =
        _ => IO.raiseError(DependencyFailure("def usersPreferences: UserId => IO[UserPreferences]", "timeout"))

      def product: ProductId => IO[Option[Product]] =
        _ => IO.raiseError(DependencyFailure("def product: ProductId => IO[Product]]", "network failure"))

      def cachedProduct: ProductId => IO[Option[Product]] =
        _ => IO.raiseError(DependencyFailure("def cachedProduct: ProductId => IO[Option[Product]]", "not responding"))

      def productPrice: Product => UserPreferences => IO[Price] =
        _ => _ => IO.raiseError(DependencyFailure("def productPrice: Product => UserPreferences => IO[Price]", "timeout"))

      def storeProductToCache: ProductId => Product => IO[Unit] =
        _ => _ => IO.raiseError(DependencyFailure("def storeProductToCache: ProductId => Product => IO[Unit]", "not responding"))
    }
}
