package external
package library

trait ThrowableMap[E] { self =>
  def map(th: Throwable): E
}
