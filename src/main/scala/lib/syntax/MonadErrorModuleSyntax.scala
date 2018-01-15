package lib.syntax

import cats.MonadError
import lib.ErrorInvariantMap
import lib.util.MonadErrorModule

import scala.language.higherKinds

object MonadErrorModuleSyntax {

  implicit final class MonadErrorModuleOps[F[_], E1](me: MonadError[F, E1]) {
    def adaptErrorType[E2](implicit EC: ErrorInvariantMap[E1, E2]): MonadError[F, E2] =
      MonadErrorModule.adaptErrorType(me)
  }
}