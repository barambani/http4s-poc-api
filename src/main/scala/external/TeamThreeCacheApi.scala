package external

import model.DomainModel
import model.DomainModel.{Product, ProductId}

import scalaz.concurrent.Task

sealed trait TeamThreeCacheApi[K, V] {
  def get: K => Task[Option[V]]
  def put: K => V => Task[Unit]
}

object TeamThreeCacheApi {

  @inline def apply(): TeamThreeCacheApi[ProductId, Product] =
    new TeamThreeCacheApi[ProductId, Product] {

      def get: DomainModel.ProductId => Task[Option[DomainModel.Product]] = ???

      def put: ProductId => Product => Task[Unit] = ???
    }
}