package model

import model.DomainModel.{ProductId, ProductIdT, UserId, UserIdT}
import shapeless.tag.@@
import shapeless.tag

object DomainModelSyntax {
  implicit def domainModelLongSyntax(l: Long) = new LongOps(l)
  implicit def domainModelValSyntax[A <: AnyVal](a: A) = new DomainModelValOps[A](a)
}

final class LongOps(l: Long) {

  import DomainModelSyntax._

  def asUserId: UserId = l.refined[UserIdT]
  def asProductId: ProductId = l.refined[ProductIdT]
}

final class DomainModelValOps[A <: AnyVal](a: A) {
  def refined[T]: A @@ T = tag[T](a)
}
