package dependencies

import model.DomainModel

import scalaz.concurrent.Task
import model.DomainModel._

sealed trait TeamThreeCacheApi[K, V] {
  def get: K => Task[Option[V]]
}

object TeamThreeCacheApi extends TeamThreeCacheApi[ProductId, Product] {
  def get: DomainModel.ProductId => Task[Option[DomainModel.Product]] = ???
}