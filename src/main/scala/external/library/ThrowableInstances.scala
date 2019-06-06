package external
package library

import cats.Show
import cats.effect.util.CompositeException

private[library] trait ThrowableInstances {

  implicit final def throwableShow: Show[Throwable] =
    new Show[Throwable] {
      def show(t: Throwable): String =
        apiErrorDecomposition(t)

      private def apiErrorDecomposition: Throwable => String = {
        case e: CompositeException => showOf(e)
        case e: Throwable          => flatten(e) mkString "\n\rcaused by "
      }

      private def showOf[E <: Throwable](e: E)(implicit ev: Show[E]): String =
        ev.show(e)
    }

  implicit final def compositeFailureShow(implicit ev: Show[Throwable]): Show[CompositeException] =
    new Show[CompositeException] {
      def show(t: CompositeException): String =
        (t.all map ev.show).toList mkString "\n"
    }

//  implicit final def throwableResponse[F[_]: Monad]: ErrorResponse[F, Throwable] =
//    new ErrorResponse[F, Throwable] {
//      val ev = Show[Throwable]
//      def responseFor: Throwable => F[Response[F]] =
//        e => InternalServerError(ev.show(e))
//    }

//  implicit final def throwableSemigroup: Semigroup[Throwable] =
//    new Semigroup[Throwable] {
//      def combine(x: Throwable, y: Throwable): Throwable =
//        CompositeException(x, y, Nil)
//    }

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
