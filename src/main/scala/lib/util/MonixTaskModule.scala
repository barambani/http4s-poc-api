package lib.util

import cats.syntax.either._
import monix.eval.Task

trait MonixTaskModule {

  def adaptError[E, A](aTask: Task[A])(errM: Throwable => E): Task[Either[E, A]] =
    aTask.attempt map (_ leftMap errM)
}

object MonixTaskModule extends MonixTaskModule