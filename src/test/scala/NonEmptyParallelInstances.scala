import cats.{Apply, FlatMap, NonEmptyParallel, ~>}

object NonEmptyParallelInstances {

  implicit def eitherNonEmptyParallel[E]: NonEmptyParallel[Either[E, ?], Either[E, ?]] =
    new NonEmptyParallel[Either[E, ?], Either[E, ?]] {
      def apply: Apply[Either[E, _]] = ???
      def flatMap: FlatMap[Either[E, _]] = ???
      def sequential: ~>[Either[E, _], Either[E, _]] = ???
      def parallel: ~>[Either[E, _], Either[E, _]] = ???
    }
}