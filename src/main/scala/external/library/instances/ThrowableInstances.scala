package external
package library
package instances

import cats.Show
import cats.effect.util.CompositeException

private[instances] trait ThrowableInstances {
  implicit final def throwableShow: Show[Throwable] =
    new Show[Throwable] {
      def show(t: Throwable): String =
        t match {
          case e: CompositeException => showOf(e)
          case e: Throwable          => flatten(e) mkString "\n\rcaused by "
        }

      private[this] def showOf[E <: Throwable](e: E)(implicit ev: Show[E]): String =
        ev.show(e)

      private[this] def flatten: Throwable => Seq[String] =
        th => {
          @scala.annotation.tailrec
          def loop(c: Option[Throwable], acc: =>Vector[String]): Vector[String] =
            c match {
              case Some(inTh) => loop(Option(inTh.getCause), acc :+ inTh.getMessage)
              case None       => acc
            }

          loop(Option(th), Vector.empty)
        }
    }

  implicit final def compositeFailureShow(implicit ev: Show[Throwable]): Show[CompositeException] =
    new Show[CompositeException] {
      def show(t: CompositeException): String =
        (t.all map ev.show).toList mkString "\n"
    }
}
