package lib

trait ErrorInvariantMap[E1, E2] {
  def direct: E1 => E2
  def reverse: E2 => E1
}

object ErrorInvariantMap {
  def apply[E1, E2](implicit E: ErrorInvariantMap[E1, E2]): ErrorInvariantMap[E1, E2] = E
}