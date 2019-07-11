package external
package library
package instances

import cats.Show
import shapeless.tag.@@

private[instances] trait TaggedInstances {

  implicit def taggedShow[A, T](implicit ev: Show[A]): Show[A @@ T] =
    new Show[A @@ T] {

      def show(t: A @@ T): String =
        ev show t
    }
}
