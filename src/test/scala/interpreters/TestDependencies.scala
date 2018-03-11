package interpreters

import cats.effect.IO
import errors.DependencyFailure
import model.DomainModel._
import cats.syntax.apply._

object TestDependencies {

  def testSucceedingDependencies(
    aUser           : User,
    preferences     : UserPreferences,
    productsInStore : Map[ProductId, Product],
    productsInCache : Map[ProductId, Product],
    price           : Price)
  (testLogger: Logger[IO]): Dependencies[IO] =
    new Dependencies[IO] {
      def user: UserId => IO[User] =
        _ => testLogger.debug("DEP user -> Getting the user in test") *> IO(Thread.sleep(1000)) *> IO(aUser)

      def usersPreferences: UserId => IO[UserPreferences] =
        _ => testLogger.debug("DEP usersPreferences -> Getting the preferences in test") *> IO(Thread.sleep(1000)) *> IO(preferences)

      def product: ProductId => IO[Option[Product]] =
        id => testLogger.debug("DEP product -> Getting the product from the repo in test") *> IO(Thread.sleep(1000)) *> IO(productsInStore.get(id))

      def cachedProduct: ProductId => IO[Option[Product]] =
        id => testLogger.debug("DEP cachedProduct -> Getting the product from the cache in test") *> IO(Thread.sleep(100)) *> IO(productsInCache.get(id))

      def productPrice: Product => UserPreferences => IO[Price] =
        _ => _ => testLogger.debug("DEP productPrice -> Getting the price in test") *> IO(Thread.sleep(1000)) *> IO(price)

      def storeProductToCache: ProductId => Product => IO[Unit] =
        _ => _ => testLogger.debug("DEP storeProductToCache -> Storing the product to the repo in test") *> IO(Thread.sleep(1000)) *> IO.unit
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
