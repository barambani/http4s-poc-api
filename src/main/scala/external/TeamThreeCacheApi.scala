package external

import cats.Functor
import model.DomainModel.{ Product, ProductId }
import scalaz.concurrent.Task

sealed trait TeamThreeCacheApi[K, V] {
  def get: K => Task[Option[V]]
  def put: K => V => Task[Unit]
}

object TeamThreeCacheApi extends TeamThreeCacheApiInstances {

  @inline def apply[K, V](implicit INST: TeamThreeCacheApi[K, V]): TeamThreeCacheApi[K, V] = INST

  implicit def productCache: TeamThreeCacheApi[ProductId, Product] =
    new TeamThreeCacheApi[ProductId, Product] {
      def get: ProductId => Task[Option[Product]] = ???
      def put: ProductId => Product => Task[Unit] = ???
    }
}

sealed private[external] trait TeamThreeCacheApiInstances {

  implicit val scalazTaskFunctor: Functor[Task] =
    new Functor[Task] {
      def map[A, B](fa: Task[A])(f: A => B): Task[B] = fa map f
    }
}
