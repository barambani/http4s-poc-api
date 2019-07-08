package external

import cats.effect.IO
import model.DomainModel.{ Product, ProductId }

trait TeamThreeCacheApi[K, V] {
  def get: K => IO[Option[V]]
  def put: K => V => IO[Unit]
}

object TeamThreeCacheApi {

  def productCache: TeamThreeCacheApi[ProductId, Product] =
    new TeamThreeCacheApi[ProductId, Product] {
      def get: ProductId => IO[Option[Product]] = ???
      def put: ProductId => Product => IO[Unit] = ???
    }
}
