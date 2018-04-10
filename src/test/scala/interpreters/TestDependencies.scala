package interpreters

import cats.effect.{IO, Timer}
import cats.syntax.apply._
import errors.DependencyFailure
import model.DomainModel._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object TestDependencies {

  def testSucceedingDependencies(
    aUser           : User,
    preferences     : UserPreferences,
    productsInStore : Map[ProductId, Product],
    productsInCache : Map[ProductId, Product],
    price           : Price)
  (testLogger: Logger[IO])(implicit ec: ExecutionContext): Dependencies[IO] =
    new Dependencies[IO] {
      def user: UserId => IO[User] =
        _ => IO.shift *> testLogger.debug("DEP user -> Getting the user in test") *> Timer[IO].sleep(1.second) *> IO(aUser)

      def usersPreferences: UserId => IO[UserPreferences] =
        _ => IO.shift *> testLogger.debug("DEP usersPreferences -> Getting the preferences in test") *> Timer[IO].sleep(1.second) *> IO(preferences)

      def product: ProductId => IO[Option[Product]] =
        id => IO.shift *> testLogger.debug("DEP product -> Getting the product from the repo in test") *> Timer[IO].sleep(1.second) *> IO(productsInStore.get(id))

      def productPrice: Product => UserPreferences => IO[Price] =
        _ => _ => IO.shift *> testLogger.debug("DEP productPrice -> Getting the price in test") *> Timer[IO].sleep(1.second) *> IO(price)

      def cachedProduct: ProductId => IO[Option[Product]] =
        id => IO.shift *> testLogger.debug("DEP cachedProduct -> Getting the product from the cache in test") *> Timer[IO].sleep(200.milliseconds) *> IO(productsInCache.get(id))

      def storeProductToCache: ProductId => Product => IO[Unit] =
        _ => _ => IO.shift *> testLogger.debug("DEP storeProductToCache -> Storing the product to the repo in test") *> Timer[IO].sleep(200.milliseconds) *> IO.unit
    }

  def testFailingDependencies(implicit ec: ExecutionContext): Dependencies[IO] =
    new Dependencies[IO] {
      def user: UserId => IO[User] =
        _ => Timer[IO].sleep(200.milliseconds) *> IO(throw DependencyFailure("def user: UserId => IO[User]", "network failure"))

      def usersPreferences: UserId => IO[UserPreferences] =
        _ => Timer[IO].sleep(100.milliseconds) *> IO(throw DependencyFailure("def usersPreferences: UserId => IO[UserPreferences]", "timeout"))

      def product: ProductId => IO[Option[Product]] =
        _ => Timer[IO].sleep(400.milliseconds) *> IO(throw DependencyFailure("def product: ProductId => IO[Product]]", "network failure"))

      def cachedProduct: ProductId => IO[Option[Product]] =
        _ => Timer[IO].sleep(300.milliseconds) *> IO(throw DependencyFailure("def cachedProduct: ProductId => IO[Option[Product]]", "not responding"))

      def productPrice: Product => UserPreferences => IO[Price] =
        _ => _ => Timer[IO].sleep(600.milliseconds) *> IO(throw DependencyFailure("def productPrice: Product => UserPreferences => IO[Price]", "timeout"))

      def storeProductToCache: ProductId => Product => IO[Unit] =
        _ => _ => Timer[IO].sleep(150.milliseconds) *> IO(throw DependencyFailure("def storeProductToCache: ProductId => Product => IO[Unit]", "not responding"))
    }
}
