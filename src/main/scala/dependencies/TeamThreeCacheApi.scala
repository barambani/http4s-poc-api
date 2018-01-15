package dependencies

import scalaz.concurrent.Task

sealed trait TeamThreeCacheApi[K, V] {
  def get: K => Task[Option[V]]
}