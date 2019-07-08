import zio.interop.ParIO

package object service {
  final type ParTask[A] = ParIO[Any, Throwable, A]
}
