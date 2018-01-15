package lib.syntax

import lib.util.FutureModule

import scala.concurrent.{ExecutionContext, Future}

trait FutureModuleSyntax {

  implicit final class FutureModuleOps[A](aFuture: => Future[A]) {

    def adaptError[E](errM: Throwable => E)(implicit ec: ExecutionContext): Future[Either[E, A]] =
      FutureModule.adaptError(aFuture)(errM)
  }
}

object FutureModuleSyntax extends FutureModuleSyntax