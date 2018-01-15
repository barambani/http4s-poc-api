package lib.syntax

import lib.util.MonixTaskModule
import monix.eval.Task

trait MonixTaskModuleSyntax {

  implicit final class TaskModuleOps[A](aTask: => Task[A]) {

    def adaptError[E](errM: Throwable => E): Task[Either[E, A]] =
      MonixTaskModule.adaptError(aTask)(errM)
  }
}

object MonixTaskModuleSyntax extends MonixTaskModuleSyntax
