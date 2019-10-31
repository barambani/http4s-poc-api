package external
package library

trait newtype[A] {
  type Base <: Any
  private[newtype] trait Tag extends Any
  type T <: Base with Tag

  final def apply(a: A): T =
    a.asInstanceOf[T]

  final def mkF[F[_]](fa: F[A]): F[T] =
    fa.asInstanceOf[F[T]]

  final def unMkF[F[_]](ft: F[T]): F[A] =
    mkF[λ[α => F[α] => F[A]]](identity)(ft)
}

object newtype {
  def apply[A]: newtype[A] = new newtype[A] {}

  implicit final class NewTypeSyntax[A](private val t: newtype[A]#T) extends AnyVal {
    def unMk: A = t.asInstanceOf[A]
  }
}
