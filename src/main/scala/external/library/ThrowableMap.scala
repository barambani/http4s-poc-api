package external
package library

trait ThrowableMap[E] {
  def map(th: Throwable): E
}
